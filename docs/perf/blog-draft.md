# 블로그 정리 초안 — 반응 API 부하 테스트 4단계

> 이 파일은 뼈대다. `(측정 후 채움)`으로 표시된 자리를 재측정 실측치로 채우고, 스크린샷은
> k6 웹 대시보드·AWS 콘솔에서 직접 캡처해 넣는다. 병목 분석 서술은 이전 실습에서 이미
> 정리한 내용을 그대로 가져왔으므로, 재측정 후 결론이 달라지는 부분만 고치면 된다.

## 용어 정리

- **처리량(Throughput)**: 서비스가 1초당 처리할 수 있는 트래픽 양
- **TPS(Transaction Per Second)**: 1초당 처리한 트랜잭션 수
- **지연시간(Latency)**: 요청에 대한 응답 시간

## 부하 테스트 결과를 보는 법 — k6 웹 대시보드 3가지 지표

파레토의 법칙에 따라 아래 3가지만 보면 된다.

1. **HTTP Request Rate** — 1초당 처리한 요청 수 = Throughput. VU를 늘려도 더 이상
   증가하지 않는 지점이 현재 시스템의 최대 Throughput이다.
2. **HTTP Request Duration** — 요청당 평균 응답 시간 = Latency. VU가 늘어날수록 서버가
   처리하지 못한 요청이 대기하면서, Throughput은 늘지 않는데 Latency만 늘어나는 현상이
   나타난다.
3. **HTTP Request Failed** — 요청 실패 수. 실패가 있다면 원인을 반드시 분석한다.

해석 순서: ① Rate가 더 이상 늘지 않는 지점을 최대 Throughput으로 본다 → ② Duration이
비정상적으로 높지 않은지 확인한다(기준은 서비스 성격에 따라 정한다, 예: 1초 초과 시
이탈률 급증) → ③ Failed가 있으면 원인을 분석한다.

## k6를 고른 이유

- 메모리를 적게 쓰면서 비교적 많은 요청을 보낼 수 있음
- 사용법이 간단함

## 측정 환경을 운영과 분리한 이유

- 운영 EC2는 Redis·RabbitMQ가 앱과 같은 도커 네트워크에 있어 부하가 전체로 번짐
- 스토어에 출시된 서비스라 실사용자 장애로 이어짐
- blue-green이 같은 인스턴스를 써서 배포가 측정을 오염시킴
- 로컬(가정용 인터넷)에서 쏘면 서버 한계가 아니라 내 업로드 대역폭·왕복 지연을 재게 됨

측정 목표 전략:
- 운영 환경과 비슷하게 데이터를 세팅한다 (측정 규모·데이터 건수는 [`docs/perf/README.md`](./README.md) 참고, 운영과의 정확한 차이도 함께 명시한다)
- 프로덕션과 분리된 환경에서 테스트한다

## 대상 API

- `POST /records/{recordId}/reaction` — 활동 기록에 반응 남기기 (이 글의 대상)
- (참고) `GET /records/stories` — 소식페이지 기록 목록 조회. 여러 유저가 동시에 자주
  호출하는 피드성 API라 트래픽이 몰릴 가능성이 높지만, 이 글에서는 다루지 않는다.

우선순위 판단 기준: 호출 빈도, 동시성 가능성(여러 명이 동시에 같은 데이터 접근),
데이터 정합성(틀리면 안 되는 수치), 응답 속도 민감도.

## 1단계: 기준선 (동기 DB, 인덱스 없음)

```js
// scripts/k6/reaction-test.js (uk_record_user_type DROP 상태로 실행)
```

결과: `http_reqs (측정 후 채움)` — 1초당 `(측정 후 채움)`건을 처리.

### 코드 분석 — `reactToRecord()`의 병목 우려 지점

```java
@Transactional
public ReactToRecordResDto reactToRecord(Long recordId, RecordReactionType type, CustomUserDetails user) {
    User currentUser = userUtil.getCurrentUser(user); // socialId로 조회
    ReportActivityRecordDto record = activityRecordUtil.getValidRecord(recordId);
    activityRecordUtil.validateAccess(...);
    validateDuplicateReaction(recordId, currentUser.getId(), type);
    // ... 반응 저장 + 카운트 증가 + 랭킹 갱신 + 알림
}
```

1. `userUtil.getCurrentUser(user)` — socialId로 유저 조회, DB 조회 병목 우려
2. `getValidRecord(recordId)` — PK 조회라 빠르지만, 지연 로딩을 막기 위해 User를
   미리 조인
3. `validateAccess(...)` — 현재 유저 ≠ 작성자일 때만 실행, 친구 관계 조회 병목 우려
4. `validateDuplicateReaction(...)` — 이미 반응 남겼는지 `existsBy...` DB 조회, 병목 우려
5. 반응 저장(`insert`) — 이후 Redis Write-Back으로 이동
6. 카운트 증가(`update`) — 이후 Redis Write-Back으로 이동

## 2단계: 복합 인덱스 추가

`activity_record_reactions`의 `uk_record_user_type` 유니크 제약(=인덱스)을 재생성한
상태로 재측정한다.

결과: `http_reqs (측정 후 채움)` — 1단계 대비 `(측정 후 채움)`배.

## 3단계: Redis Write-Back 큐 적용 (분산 락 없음)

중복확인은 여전히 DB `existsBy...` 조회지만, insert/count 반영을 Redis 큐로 비동기
처리한다(`QueueOnlyReactionMeasurementService`, `/records/{recordId}/reaction/measure/queue-only`).

결과: `http_reqs (측정 후 채움)` — 1단계 대비 `(측정 후 채움)`배.

### 더 개선할 포인트로 짚었던 것

**1. 중복 체크(`validateDuplicateReaction`)** — 비동기로 저장하기로 했지만 중복 체크를
위해 매번 DB를 조회해서, 요청마다 DB I/O가 발생해 TPS 상승에 한계가 있다.

- 대안으로 Redis Set에 저장하는 방식도 검토했으나(`reaction:check:{recordId}:{type}` →
  `userId`), 사용자 수 x 기록 수 x 반응 유형이 쌓이면 메모리 사용량이 기하급수적으로
  늘어날 것으로 예상돼 채택하지 않았다. Bitmap도 userId가 UUID라 불가능.

**2. 권한·유효성 체크 캐싱(`getValidRecord`, `validateAccess`)** — 기록·친구 관계
정보를 캐싱하면 DB 호출을 없앨 수 있지만, 이번 라운드에서는 분산 락(4단계) 쪽이 더
직접적인 병목 해소책이라 판단해 우선순위를 뒤로 미뤘다.

## 4단계: Redis 분산 락(SETNX) 적용

중복확인·큐 push 모두 `ReactionRedisLockService`(SETNX + TTL 5초)를 거친다. 운영과
동일한 v2 경로(`/api/v2/records/{recordId}/reaction`).

결과: `http_reqs (측정 후 채움)` — 1단계 대비 `(측정 후 채움)`배.

### 왜 SETNX가 중복을 막는가

**클라이언트 연타 시나리오**: 사용자가 반응 버튼을 여러 번 누르면(네트워크 지연,
화면 미반응 등) 짧은 시간차로 여러 요청이 도착한다. 비동기 큐(Write-Back) 구조에서
Redis 체크가 없다면 같은 데이터가 큐에 중복으로 들어가거나, 나중에 스케줄러가 DB에
반영할 때 유니크 제약 위반으로 벌크 저장이 실패할 수 있다.

**원자성**: Redis는 싱글 스레드로 명령을 처리하므로, 1ms 안에 100개 요청이 몰려도
'누가 먼저 왔는지' 순서가 명확하다 — 가장 먼저 온 요청만 락 생성에 성공(`true`)하고
나머지는 `false`를 받아 즉시 예외로 거절된다.

**찰나의 락**: `Duration.ofSeconds(5)`로 TTL을 짧게 둬서, 반응 데이터 자체가 아니라
"방금 반응했다"는 사실만 5초간 기억한다. 이 덕분에 중복 데이터가 큐에 들어가는 것 자체를
막고, 메모리 점유도 낮게 유지된다. 설령 중복이 큐에 들어가더라도 DB의 유니크 제약이
최종 방어선이 된다.

## 종합 비교

| 단계 | 처리량(req/s) | p50 (ms) | p95 (ms) | p99 (ms) | 실패율 |
| --- | --- | --- | --- | --- | --- |
| ① 기준선 | (측정 후 채움) | | | | |
| ② 인덱스 추가 | (측정 후 채움) | | | | |
| ③ Redis Write-Back | (측정 후 채움) | | | | |
| ④ Redis 분산 락 | (측정 후 채움) | | | | |

병목 위치(EC2 CPU/메모리 vs RDS CPU/메모리/IOPS)도 CloudWatch 콘솔 캡처와 함께 단계별로
남긴다.

## 남은 병목과 다음 단계

- `activity_record_reactions.existsBy...` DB 조회는 4단계에서도 v1 경로(`/records/{id}/reaction`)에는 그대로 남아 있다 — v2(락) 경로로 트래픽을 전면 전환하지 않는 한 사라지지 않는다.
- Redis Write-Back 큐(`ReactionScheduler`)는 1초마다 최대 1,000건만 MySQL로 내려쓴다. 유입률이 이 상한을 넘으면 큐가 무한히 쌓인다 — 포화점 측정은 `#376`에서 스파이크 시나리오로 별도로 다룬다.
