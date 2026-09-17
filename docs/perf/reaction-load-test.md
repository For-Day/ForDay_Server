# 반응(reaction) API 4단계 재측정 (이슈 #375)

측정 환경은 [`docs/perf/README.md`](./README.md) 참고 — 이 문서는 반응 API 재측정 절차만
다룬다.

## 기존 수치를 재측정하는 이유

기존 k6 스크립트(`reaction-test.js`, `reaction-test-redis.js`)는 `setup()`에서 게스트
토큰을 **1개만** 발급해 1,000 VU 전부가 같은 유저로 요청했다. `recordId(1~100) x type(4)`
= 400개 조합뿐이라, 유니크 제약(`uk_record_user_type`)에 걸려 대부분의 요청이 실제 쓰기
경로가 아니라 `DUPLICATE_REACTION`(400) 거절 경로를 탄 채로 측정됐을 가능성이 크다. 즉
기존 106 → 598 → 1,108 req/s는 "쓰기 처리량"이 아니라 "중복 거절 응답 처리량"에 가까울
수 있다.

## 4단계 → 코드/DDL 매핑

같은 서버 인스턴스에서 "인덱스 있음"과 "인덱스 없음"을 동시에 살려둘 수 없다(인덱스는
테이블 전체에 적용되는 물리적 속성). 그래서 인덱스 단계(①→②)만 측정 당일 DDL로
전환하고, 나머지는 코드 경로(엔드포인트)로 즉시 전환한다. git checkout은 쓰지 않는다.

| 단계 | 내용 | 엔드포인트 | 전환 방법 |
| --- | --- | --- | --- |
| ① 기준선 | 동기 DB, 중복확인 인덱스 없음 | `POST /records/{recordId}/reaction` (v1) | 측정 전 `uk_record_user_type` 제약 DROP |
| ② 인덱스 추가 | 동기 DB, 인덱스 있음 | 동일 (v1) | ① 측정 후 제약 재생성 |
| ③ Redis Write-Back 큐만 | 중복확인 DB, insert/count는 Redis 큐 | `POST /records/{recordId}/reaction/measure/queue-only` (`measure` 프로파일) | 즉시 |
| ④ Redis 분산 락 | 중복확인·큐 모두 Redis(SETNX) | `POST /api/v2/records/{recordId}/reaction` (v2) | 즉시 |

②·④는 기존 라이브 코드 그대로다. ③만 이번에 `QueueOnlyReactionMeasurementService`로
새로 추가했다 — `ReactionRedisLockService.createReactionWithRedis`에서 SETNX 락 체크만
뺀 버전이고, 큐 드레인(`ReactionScheduler`)은 두 경로가 공유한다.

## 실행 순서 (반드시 이 순서로)

①이 유일하게 파괴적 DDL(제약 DROP)이 필요한 단계이므로 가장 먼저, 한 번만 실행한다.

```bash
# 0) 측정 대상 EC2에 앱을 `measure` 프로파일로 기동, ReactionMeasurementSeeder 시드 완료 확인

# 1) ① 기준선 - 인덱스 없는 상태로 전환
mysql -h <측정용-RDS엔드포인트> -u <user> -p forday \
  -e "ALTER TABLE activity_record_reactions DROP INDEX uk_record_user_type;"

K6_WEB_DASHBOARD=true k6 run --summary-export=docs/perf/results/stage1/run-1.json scripts/k6/reaction-test.js
K6_WEB_DASHBOARD=true k6 run --summary-export=docs/perf/results/stage1/run-2.json scripts/k6/reaction-test.js
K6_WEB_DASHBOARD=true k6 run --summary-export=docs/perf/results/stage1/run-3.json scripts/k6/reaction-test.js

# 2) ② 인덱스 추가 - 제약 재생성
mysql -h <측정용-RDS엔드포인트> -u <user> -p forday \
  -e "ALTER TABLE activity_record_reactions ADD CONSTRAINT uk_record_user_type UNIQUE (activity_record_id, reacted_user_id, reactionType);"

K6_WEB_DASHBOARD=true k6 run --summary-export=docs/perf/results/stage2/run-1.json scripts/k6/reaction-test.js
K6_WEB_DASHBOARD=true k6 run --summary-export=docs/perf/results/stage2/run-2.json scripts/k6/reaction-test.js
K6_WEB_DASHBOARD=true k6 run --summary-export=docs/perf/results/stage2/run-3.json scripts/k6/reaction-test.js

# 3) ③ Redis Write-Back 큐만 - 코드 경로 전환, DDL 불필요
K6_WEB_DASHBOARD=true k6 run --summary-export=docs/perf/results/stage3/run-1.json scripts/k6/reaction-test-queue-only.js
K6_WEB_DASHBOARD=true k6 run --summary-export=docs/perf/results/stage3/run-2.json scripts/k6/reaction-test-queue-only.js
K6_WEB_DASHBOARD=true k6 run --summary-export=docs/perf/results/stage3/run-3.json scripts/k6/reaction-test-queue-only.js

# 4) ④ Redis 분산 락 - 코드 경로 전환, DDL 불필요
K6_WEB_DASHBOARD=true k6 run --summary-export=docs/perf/results/stage4/run-1.json scripts/k6/reaction-test-redis.js
K6_WEB_DASHBOARD=true k6 run --summary-export=docs/perf/results/stage4/run-2.json scripts/k6/reaction-test-redis.js
K6_WEB_DASHBOARD=true k6 run --summary-export=docs/perf/results/stage4/run-3.json scripts/k6/reaction-test-redis.js
```

각 실행은 `BASE_URL`(기본 `http://localhost:8080`), `USER_COUNT`(기본 200),
`RECORD_COUNT`(기본 1000) 환경변수로 조절 가능 — `ReactionMeasurementSeeder`가 시드한
값과 반드시 맞춰야 한다(`-e USER_COUNT=200 -e RECORD_COUNT=1000` 형태로 전달).

## 기존 수치 vs 재측정치

| 단계 | 기존(신뢰 불가) | 재측정(중위값) | 성공 요청 비율 | 차이 원인 |
| --- | --- | --- | --- | --- |
| ① 기준선 | 106 req/s | (측정 후 채움) | (측정 후 채움) | |
| ② 인덱스 추가 | ~200 req/s | (측정 후 채움) | (측정 후 채움) | |
| ③ Redis Write-Back | 598 req/s | (측정 후 채움) | (측정 후 채움) | |
| ④ Redis 분산 락 | 1,108 req/s | (측정 후 채움) | (측정 후 채움) | |

"성공 요청 비율"은 `<prefix>_success` Counter 합계를 전체 `http_reqs`로 나눈 값 —
기존 수치가 실제로 중복 거절 경로를 재고 있었는지 확인하는 핵심 지표다. 이 비율이
낮다면(예: 30% 미만) 기존 수치는 재측정치로 완전히 대체하고, 그 이유를 블로그에 명시한다.
