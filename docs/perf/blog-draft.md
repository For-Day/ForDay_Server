# 반응(reaction) API 부하 테스트 — 문제 진단부터 4단계 개선까지

> 수치는 전부 측정 전용 환경(#374)에서 3회 반복 측정한 실측치다(중위값 기준).
> `(직접 채우기)` 표시된 자리만 본인 해석·스크린샷을 채우면 된다.

## 목표

- **대상 API**: `POST /records/{recordId}/reaction` — 활동 기록에 반응 남기기
- **우선순위 판단 기준**: 호출 빈도, 동시성 가능성(여러 명이 동시에 같은 데이터 접근), 데이터
  정합성(틀리면 안 되는 수치), 응답 속도 민감도. 네 조건을 다 만족하는 API라 첫 부하테스트
  대상으로 골랐다.
- **목표 TPS**: 기준선(Step 1) 대비 **5배 이상**. 인덱스 추가·비동기 큐·분산 락까지 다
  넣으면 그 정도는 나올 거라고 예상하고 시작했다.
- **테스트 환경**: 운영 EC2와 동일한 AMI·인스턴스 타입(t3.small)에 앱·Redis·RabbitMQ를
  올리고, RDS도 운영과 같은 엔진 버전(MySQL 8.0.43)으로 맞춘 별도 인스턴스를 썼다. 운영
  EC2에 직접 부하를 걸지 않은 이유는 아래 참고.

## 왜 운영 환경에 바로 부하를 걸지 않았나

- 운영 EC2는 Redis·RabbitMQ가 앱과 같은 도커 네트워크에 있어 부하가 서비스 전체로 번진다
- 스토어에 이미 출시된 서비스라 실사용자 장애로 이어진다
- blue-green이 같은 인스턴스를 써서, 측정 중 배포가 일어나면 컨테이너 교체로 측정이 오염된다
- 로컬(가정용 인터넷)에서 EC2로 쏘면 서버 한계가 아니라 내 업로드 대역폭·왕복 지연을 재게 된다

그래서 운영과 동일한 스펙의 **측정 전용 EC2(대상)** + **측정 전용 EC2(k6 발생기)** +
**별도 RDS**를 새로 띄워서 측정했다. 시드 데이터는 게스트 유저 200명, 기록 1,000건.

## k6를 고른 이유

- 메모리를 적게 쓰면서 비교적 많은 요청을 보낼 수 있음
- 사용법이 간단함

## 부하 테스트 결과를 보는 법

k6가 보여주는 값이 많아 보이지만, 파레토의 법칙에 따라 아래 3가지만 보면 된다.

1. **HTTP Request Rate** — 1초당 처리한 요청 수 = Throughput(TPS). VU를 늘려도 더 이상
   증가하지 않는 지점이 현재 시스템의 최대 Throughput이다.
2. **HTTP Request Duration** — 요청당 응답 시간 = Latency. VU가 늘어날수록 서버가 처리
   못한 요청이 대기하면서, Throughput은 안 느는데 Latency만 늘어나는 현상이 나타난다.
3. **HTTP Request Failed** — 요청 실패 수. 실패가 있으면 원인을 반드시 분석한다.

해석 순서: ① Rate가 더 이상 늘지 않는 지점을 최대 Throughput으로 본다 → ② Duration이
비정상적으로 높지 않은지 확인한다 → ③ Failed가 있으면 원인을 분석한다.

**두 가지 숫자를 구분해서 본다** — k6의 `http_reqs`(raw)는 `DUPLICATE_REACTION`(400)
거절 응답까지 포함한 전체 처리량이다. "새 반응이 실제로 기록된" 처리량은 상태코드별
Counter(`*_success`)로 따로 뽑았다. 아래 모든 단계에서 이 둘을 같이 적는다.

---

# 1. 기록에 반응 남기기

## Step 1 — 기준선 측정

동기 처리 경로(`ReactionService.reactToRecord`)를 그대로 부하테스트했다.

```js
// scripts/k6/reaction-test.js
import http from 'k6/http';
import { check } from 'k6';
import { setupGuestTokens, pickTarget, makeStatusCounters, tagStatus, BASE_URL } from './common.js';

export const options = {
  vus: 1000,
  duration: '10s',
};

const counters = makeStatusCounters('v1');

// 시드된 게스트 유저(measure_user_1..200) 각각으로 로그인해 서로 다른 토큰 200개를 발급한다.
// (실수했던 첫 시도: setup()에서 토큰을 1개만 발급했더니 1,000 VU가 전부 같은 유저로
//  요청하는 꼴이 돼서, 대부분 DUPLICATE_REACTION 거절 응답 속도를 재고 있었다 — 아래
//  "측정 스크립트 자체의 버그" 참고)
export function setup() {
  return { tokens: setupGuestTokens() };
}

export default function (data) {
  const { userIndex, recordId, reactionType } = pickTarget(__VU, __ITER);
  const token = data.tokens[userIndex];

  const res = http.post(
      `${BASE_URL}/records/${recordId}/reaction`,
      JSON.stringify({ reactionType }),
      { headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` } }
  );

  tagStatus(counters, res.status);
  check(res, { '2xx': (r) => r.status >= 200 && r.status < 300 });
}
```

```bash
K6_WEB_DASHBOARD=true k6 run --summary-export=stage1-summary.json scripts/k6/reaction-test.js
```

**결과 (3회 측정, 중위값)**

| 지표 | run1 | run2 | run3 | 중위값 |
| --- | --- | --- | --- | --- |
| Raw TPS (`http_reqs`) | 189.7 | 231.9 | 252.0 | **231.9** |
| 보정 TPS (`v1_success`, 실제 반영된 요청만) | 167.9 | 208.7 | 210.4 | **208.7** |
| Request Duration p95 | 5,399ms | 4,648ms | 4,346ms | **4,648ms** |

- 측정 대상 EC2 CPU: 평균 32%, **최대 75%**
- RDS CPU: 4~37%(낮음) / RDS 여유 메모리: 107~127MB(db.t4g.micro 1GB 중 — 빠듯)

`http_reqs: 231.9/s` — 목표(기준선의 5배, 약 1,044/s)까지는 한참 남았다는 걸 첫 측정부터
알 수 있었다.

---

## reactToRecord() 코드에서 병목이 우려되는 부분 짚어보기

```java
@Transactional
public ReactToRecordResDto reactToRecord(Long recordId, RecordReactionType type, CustomUserDetails user) {
    User currentUser = userUtil.getCurrentUser(user); // socialId로 유저 조회

    ReportActivityRecordDto record = activityRecordUtil.getValidRecord(recordId);
    if (!isRecordOwner(currentUser, record)) {
        activityRecordUtil.validateAccess(currentUser.getId(), record.getWriterId(), record.isWriterDeleted(), record.getVisibility());
    }
    validateDuplicateReaction(recordId, currentUser.getId(), type);

    ActivityRecordReaction reaction = ActivityRecordReaction.of(activityRecordRepository.getReferenceById(recordId), userRepository.getReferenceById(currentUser.getId()), type);
    recordReactionRepository.save(reaction);

    int result = recordReactionCountRepository.increaseCount(recordId, type.toString());
    if (result == 0) {
        recordReactionCountRepository.save(ActivityRecordReactionCount.init(recordId, type));
    }
    reactionRankingService.incrementRankingScore(record.getRecordId());

    if (!isRecordOwner(currentUser, record)) {
        notificationService.processReactionNotification(currentUser, userRepository.getReferenceById(record.getWriterId()), type, record.getRecordId(), record.getImageUrl());
    }

    return ReactToRecordResDto.of(type, recordId);
}
```

한 줄씩 "이게 왜 느릴 수 있는가"를 따져봤다.

1. **`userUtil.getCurrentUser(user)`** — `socialId`로 유저를 조회한다. `users` 테이블에
   `uk_users_social_id` 유니크 인덱스가 이미 있어서(예전 이슈에서 추가됨) 이 줄 자체는
   병목이 아니었다 — 확인 결과 제외.
2. **`getValidRecord(recordId)`** — `recordId`는 PK라 조회는 빠르다. 다만 나중에 작성자
   정보가 필요해서 `User`를 미리 조인해온다.
3. **`validateAccess(...)`** — 현재 유저가 작성자가 아닐 때만 실행되는 친구 관계 조회.
   `friend_relations`에 `idx_requester_target`/`idx_target_requester` 복합 인덱스가 이미
   있어서 이 줄도 제외.
4. **`validateDuplicateReaction(...)`** — `existsByRecordIdAndUserIdAndType`으로 이미
   반응을 남겼는지 DB에 물어본다. **여기가 진짜 의심 지점이었다** — `activity_record_reactions`
   테이블에 `(activity_record_id, reacted_user_id, reaction_type)` 복합 인덱스가 없어서,
   요청마다 이 조회가 비효율적인 스캔에 가까웠을 것으로 추정.
5. **반응 저장(`insert`)** — 나중에 Redis Write-Back으로 옮길 대상.
6. **카운트 증가(`update`/`insert`)** — 이것도 나중에 Redis Write-Back으로 옮길 대상.

`(직접 채우기 — 예시: EC2 CPU가 75%까지 튀고 p95가 4.6초까지 늘어난 걸 보면, 인덱스 없는
existsBy 조회가 커넥션을 오래 붙잡고 있었고 그게 EC2 CPU까지 끌어올린 것으로 보인다.
RDS 자체 CPU는 낮았던 걸 보면 "DB 서버가 바쁜 게 아니라 비효율적인 쿼리가 연결을
오래 점유해서 처리량이 막힌" 패턴에 가깝다.)`

**개선 조치**: `activity_record_reactions`에 `(activity_record_id, reacted_user_id,
reaction_type)` 복합 유니크 인덱스(`uk_record_user_type`) 추가.

---

## Step 2 — 복합 인덱스 추가 후 재측정

같은 스크립트, 같은 엔드포인트. 인덱스만 추가하고 다시 돌렸다.

| 지표 | run1 | run2 | run3 | 중위값 |
| --- | --- | --- | --- | --- |
| Raw TPS | 358.1 | 393.1 | 349.4 | **358.1** |
| 보정 TPS | 324.0 | 218.2 | 310.7 | **310.7** |
| Request Duration p95 | 3,051ms | 2,798ms | 3,184ms | **3,051ms** |

- 측정 대상 EC2 CPU: 평균 31%, 최대 **55%**(75%에서 하락)
- RDS CPU: 25~32%

`http_reqs: 358.1/s` — 기준선 대비 raw 기준 약 **1.5배**, 보정 기준 **+49%** 증가했다.
목표(5배)에는 한참 못 미치지만, 방향은 맞았다.

### 더 개선할 포인트 찾기

**1. 중복 체크(`validateDuplicateReaction`)** — 인덱스를 추가해도 여전히 요청마다 DB를
직접 왕복한다. `insert`/`update`도 마찬가지로 동기 처리라, HTTP 스레드가 DB 커밋까지
전부 들고 있는 구조 자체가 다음 병목이라고 판단했다.

- **해결 방향**: 반응 저장과 카운트 반영을 Redis Write-Back 큐로 비동기 처리한다 — 요청
  스레드는 큐에 push만 하고 즉시 응답, 실제 DB 반영은 스케줄러가 배치로 나중에 처리.

**2. 권한·유효성 체크 캐싱(`getValidRecord`, `validateAccess`)** — 검토는 했지만 이번
라운드에서는 채택하지 않았다(이유는 글 마지막 "검토했지만 채택 안 함" 참고).

**개선 조치**: Redis Write-Back 큐(`ReactionScheduler`) 도입.

---

## Step 3 — Redis Write-Back 큐 적용 (분산 락은 아직 없음)

중복확인은 여전히 DB `existsBy...` 조회지만, insert/count 반영을 Redis 큐로 비동기
처리한다(`QueueOnlyReactionMeasurementService`,
`/records/{recordId}/reaction/measure/queue-only` — 4단계 비교를 위해 이번에 만든
측정 전용 경로).

| 지표 | run1 | run2 | run3 | 중위값 |
| --- | --- | --- | --- | --- |
| Raw TPS | 243.3 | 355.5 | 159.9 | **243.3** |
| 보정 TPS (`queue_only_success`) | 103.9 | 294.8 | 146.0 | **146.0** |
| Request Duration p95 | 1,893ms | 2,107ms | 1,826ms | **1,893ms** |

- 측정 대상 EC2 CPU: 최대 68%대 → 백로그 처리 구간에서 5% 미만으로 급락
- RDS CPU: 9~21%(Step 1·2보다 오히려 낮음 — 배치 반영이 밀려서)

`http_reqs: 243.3/s` — **Step 2(358.1)보다 오히려 떨어졌다.** 예상과 정반대 결과라
원인을 파고들었다.

### 왜 떨어졌는지 — 실측으로 찾은 진짜 문제

`reaction_queue_size` 지표를 실행 중에 계속 관찰했더니, 큐가 최대 **20,521**까지
쌓이는 걸 확인했다. 응답 시간(p95 1,893ms)은 오히려 줄었는데 처리량은 반토막 난 게
모순처럼 보였지만, 실제로는 "응답은 빨리 주지만 실제 반영은 계속 밀린다"는 뜻이었다.

문제는 여기서 끝나지 않았다. 앱 로그에서 이런 에러가 반복됐다.

```
Lock wait timeout exceeded; try restarting transaction
[update users set ... where user_id=?]
```

`ReactionScheduler`가 최대 1,000건씩 묶어 `INSERT`하는 배치 트랜잭션이, FK 제약
(`reacted_user_id → users`) 때문에 해당 유저 행에 락을 걸고 있었다. 큐가 20,000건
넘게 밀린 상태에서 이 배치가 오래 걸리다 보니, **반응 기능과 전혀 무관한 게스트
로그인(유저 `last_activity_at` UPDATE)까지 최대 37초씩 블로킹**됐다. 심할 때는 k6
`setup()`의 로그인 시퀀스 전체가 60초 타임아웃으로 실패했다.

즉 "분산 락 없는 Write-Back 큐"는:
- 중복 요청까지 전부 큐에 쌓인다(락이 없어서 걸러지지 않음)
- 스케줄러는 1초에 최대 1,000건만 처리 가능한데 유입이 이걸 넘으면 큐가 무한정 쌓인다
- 쌓인 큐를 처리하는 배치가 **반응과 무관한 다른 쓰기 경로까지 마비**시킬 수 있다

**개선 조치**: Redis SETNX 기반 분산 락(`ReactionRedisLockService`)을 중복확인·큐 push
앞단에 추가 — 같은 (기록, 유저, 타입) 조합의 재요청을 TTL 5초 동안 원천 차단해서 큐
유입 자체를 억제한다.

---

## Step 4 — Redis 분산 락(SETNX) 적용

### 1. 중복이 발생할 수 있는 시나리오: "클라이언트 연타"

사용자가 반응 버튼을 누를 때, 네트워크 상태가 불안정하거나 화면이 즉시 반응하지
않으면 사용자는 버튼을 여러 번 누르게 된다.

- **T=0ms**: 첫 번째 클릭 요청이 서버에 도착
- **T=10ms**: 두 번째 클릭 요청이 서버에 도착
- **문제 발생**: 비동기 큐(Write-Back) 구조에서 Redis 체크가 없다면(Step 3처럼), 두
  요청 모두 큐에 들어가고 나중에 스케줄러가 DB에 반영할 때 유니크 제약 위반으로 벌크
  저장이 실패한다 — Step 3에서 실제로 관측했다.

### 2. Redis `setIfAbsent`(SETNX)가 중복을 막는 원리

`setIfAbsent`는 Redis의 **원자적(Atomic) 특성**을 이용한 전략이다.

**① 원자성** — Redis는 싱글 스레드로 명령을 처리하기 때문에, 1ms 안에 100개의 요청이
몰려와도 Redis 입장에서는 '누가 먼저 왔는지' 순서가 명확히 정해진다. 가장 먼저 도착한
요청은 `lockKey`를 생성하고 `true`를 반환받아 성공하고, 0.001초 뒤에 온 나머지 요청들은
이미 키가 존재하므로 `false`를 반환받아 즉시 예외로 거절된다.

**② 찰나의 락** — `Duration.ofSeconds(5)`로 TTL을 짧게 둬서, 반응 데이터 자체가 아니라
"방금 반응했다"는 사실만 5초간 기억한다. 이 덕분에 중복 데이터가 큐에 들어가는 것 자체를
막고, 메모리 점유도 낮게 유지된다. 설령 중복이 큐에 들어가더라도 DB의 유니크 제약이
최종 방어선이 된다.

### 3. 측정 결과

```js
// scripts/k6/reaction-test-redis.js — 엔드포인트만 /api/v2/records/{recordId}/reaction으로 다름
```

| 지표 | run1 | run2 | run3 | 중위값 |
| --- | --- | --- | --- | --- |
| Raw TPS | 430.8 | 399.7 | 394.2 | **399.7** |
| 보정 TPS (`redis_lock_success`) | 411.0 | 381.5 | 364.0 | **381.5** |
| Request Duration p95 | 2,664ms | 2,358ms | 2,572ms | **2,358~2,664ms** |

`http_reqs: 399.7/s` — 기준선 대비 raw 기준 약 **1.7배**, 보정 기준 **+83%** 증가.
목표였던 5배(약 1,044/s)에는 미치지 못했지만, 4단계 중 raw·보정 TPS 모두 최고치를
기록했다.

- 중복 거절(`redis_lock_duplicate`) 75~239건 — Step 1·3(수백~수천 건)과 비교하면
  압도적으로 적다. 이번엔 실패율이 높은 게 아니라 오히려 **가장 낮았다** — 락이 정확히
  "진짜 중복"만 걸러내고 큐 유입 자체를 억제했다는 뜻이다.
- 큐 적체: 3회 누적으로도 최대 **6,112**에서 멈췄다(Step 3의 20,521과 대조).

`(직접 채우기 — 예시: 처리량과 안정성을 동시에 잡은 유일한 단계였다. 다만 목표로 잡았던
5배에는 못 미쳤는데, 그 이유는 v1 경로의 existsBy 조회나 EC2 CPU 자체가 여전히 남은
병목이기 때문으로 보인다 — 아래 "남은 병목" 참고.)`

---

## 종합 비교

| 단계 | Raw TPS(중위) | 보정 TPS(중위) | p95 | 중복/락 거절 | 큐 최대 적체 |
| --- | --- | --- | --- | --- | --- |
| ① 기준선 | 231.9 | 208.7 | 4,648ms | 낮음 | - |
| ② 인덱스 추가 | 358.1 | 310.7 | 3,051ms | 낮음 | - |
| ③ Redis Write-Back만 | 243.3 | 146.0 | 1,893ms | 매우 높음 | **20,521** |
| ④ Redis 분산 락 | 399.7 | **381.5** | 2,358ms | 낮음 | 6,112 |

**목표 대비**: 기준선(208.7) 대비 5배(약 1,044)를 목표로 잡았지만, 최종 달성치는
381.5(+83%)로 **목표에 도달하지 못했다.** 정직하게 남겨두는 이유는, 이 격차 자체가
다음에 무엇을 더 해야 하는지 보여주는 지표이기 때문이다 — "남은 병목과 다음 단계" 참고.

### 측정 스크립트 자체의 버그 (재측정 전 수치와의 차이)

처음 이 4단계를 측정했을 때는 k6 스크립트의 `setup()`이 게스트 토큰을 1개만 발급해서
1,000 VU 전부가 같은 유저로 요청했다. `recordId(1~100) x type(4)` = 400개 조합뿐이라,
대부분의 요청이 실제 쓰기 경로가 아니라 `DUPLICATE_REACTION`(400) 거절 응답을 처리한
속도였을 가능성이 크다. 위 표의 수치는 게스트 유저 200명을 실제로 발급하고 (기록,
유저, 타입) 조합 충돌을 최소화하도록 스크립트를 고친 뒤 재측정한 값이다.

## 남은 병목과 다음 단계

- `activity_record_reactions.existsBy...` DB 조회는 4단계에서도 v1 경로(`/records/{id}/reaction`)에는 그대로 남아 있다 — v2(락) 경로로 트래픽을 전면 전환하지 않는 한 사라지지 않는다.
- Redis Write-Back 큐(`ReactionScheduler`)는 1초마다 최대 1,000건만 MySQL로 내려쓴다. Step 4에서도 3회 누적으로 6,112까지 쌓인 걸 보면, 이 상한 자체가 언젠가는 병목이 될 수 있다 — 정확한 포화 유입률 측정은 별도 스파이크 테스트 이슈에서 다룬다.
- 목표 5배에 못 미친 만큼, EC2 인스턴스 확장이나 `getValidRecord`/`validateAccess` 캐싱 같은 다음 레버가 남아 있다.
- 이번 측정 중 별도로 발견한 사전 존재 버그: `record_reaction_count` 초기 행 생성(`increaseCount` 실패 시 `save`)이 동시 요청 시 MySQL 데드락을 일으킨다(check-then-insert 경쟁 상태) — `INSERT ... ON DUPLICATE KEY UPDATE` 원자적 upsert로 수정.

## 더 개선할 수 있었던 포인트 (검토했지만 채택 안 함)

**중복 체크를 Redis Set으로**: `reaction:check:{recordId}:{type}` → `userId` Set에
저장하는 방식도 검토했으나, 사용자 수 x 기록 수 x 반응 유형이 쌓이면 메모리 사용량이
기하급수적으로 늘어날 것으로 예상돼 채택하지 않았다. Bitmap도 userId가 UUID라 불가능.

**권한·유효성 체크 캐싱**(`getValidRecord`, `validateAccess`): 기록·친구 관계 정보를
캐싱하면 DB 호출을 없앨 수 있지만, 분산 락 쪽이 더 직접적이고 안정성까지 잡는 해법이라
우선순위를 뒤로 미뤘다. 목표 TPS에 못 미친 지금, 다음 라운드에서 시도해볼 1순위 후보다.
