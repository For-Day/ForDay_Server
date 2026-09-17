# ADR-0002. 알림 발행 구조 — AFTER_COMMIT 직접 발행에서 Outbox 패턴으로

| 항목 | 내용 |
| --- | --- |
| 상태 | 채택 (Accepted) — 구현 완료 |
| 작성일 | 2026-09-16 |
| 대상 | `ForDay_Server` 알림 발행 경로 (`NotificationService`, `NotificationOutboxRelay`, `NotificationConsumer`) |
| 관련 이슈 | #372 (원래 "ADR 문서 작성"으로 스코프됐으나, 판단 과정에서 실제 구현까지 확장) |

---

## 1. 맥락

### 1.1 기존 구조

리액션 알림은 다음 경로로 발행됐다.

```
[reactToRecord 트랜잭션]
  → Notification DB 저장
  → (커밋 후) NotificationEventListener.handleNotificationEvent()  (@TransactionalEventListener AFTER_COMMIT)
      → rabbitTemplate.convertAndSend(...)   ← try/catch 없음, 1회 시도
        ↓ 성공 시
[RabbitMQ 큐]
  → NotificationConsumer.consumeRecordNotification()
      → for (token) { pushSenderPort.send(...) }  ← 실패는 로그만, 재시도 없음
```

"DB는 롤백됐는데 알림은 발송되는" 문제는 `AFTER_COMMIT`으로 이미 막혀 있었다(#370에서 테스트로 고정). 이 ADR이 다루는 건 그 반대 방향 — **DB에는 커밋됐는데 발행이 실패하는 경우**다.

### 1.2 발견한 문제 — 이슈 원문보다 구체적인 증상

이슈 #372 원문은 "커밋 직후 브로커 장애·애플리케이션 종료 시 알림이 유실될 수 있다"까지만 적고 있었다. 실제로 Spring 소스(`AbstractPlatformTransactionManager.processCommit()`)를 확인한 결과, 증상은 "유실"보다 나빴다.

```java
// AbstractPlatformTransactionManager.java
// Trigger afterCommit callbacks, with an exception thrown there
// propagated to callers but the transaction still considered as committed.
try {
    triggerAfterCommit(status);
}
finally {
    triggerAfterCompletion(status, TransactionSynchronization.STATUS_COMMITTED);
    ...
}
```

`TransactionSynchronizationUtils.invokeAfterCommit()`은 콜백에서 던진 예외를 삼키지 않고 그대로 호출자에게 전파한다. `NotificationEventListener`에 try/catch가 없었으므로, RabbitMQ가 순간적으로 끊기면:

1. `rabbitTemplate.convertAndSend(...)`가 `AmqpException`을 던진다.
2. 이 예외가 `reactToRecord`를 감싼 트랜잭션 커밋 호출 밖으로 전파된다.
3. `GlobalExceptionHandler`의 `Exception.class` 핸들러가 이를 잡아 **HTTP 500(`INTERNAL_SERVER_ERROR`)** 을 반환한다.

**리액션은 DB에 이미 정상 커밋됐는데, 클라이언트는 실패로 인식한다.** 재시도가 없다는 것보다, 성공을 실패로 오인시킨다는 게 더 나쁜 증상이었다.

### 1.3 실제로 얼마나 자주 발생하는가 — 측정 없이 과장하지 않기

배포 워크플로(`deploy.yml`, `docker-compose-blue.yml`)를 확인한 결과, 블루-그린 배포가 재시작하는 건 `blue`/`green` 앱 컨테이너뿐이고 **RabbitMQ·Redis는 `forday-net`에 별도로 떠 있는 장수 컨테이너**다. 즉 이 문제는 매 배포마다 발생하는 게 아니라, RabbitMQ 컨테이너 자체의 크래시·EC2 재부팅·리소스 임계치 초과 같은 드문 이벤트에서만 발생한다. 지금은 Actuator/모니터링이 없어(#373) 실제 발생 빈도를 관측할 수 없다 — 이 ADR은 "자주 터진다"가 아니라 "터지면 이렇게 나쁘다"를 근거로 판단을 정당화한다.

---

## 2. 고려한 대안

두 개의 다른 문제를 먼저 구분한다.

| 문제 | 원인 |
| --- | --- |
| **A. 성공했는데 500이 보임** | 리스너에 try/catch가 없어 예외가 트랜잭션 밖으로 전파됨 |
| **B. 알림이 조용히 유실되고 재시도가 없음** | 발행 실패를 아무도 재시도하지 않음 |

### 대안 1. 현행 유지

- **기각 사유**: 문제 A·B 모두 그대로 남는다.

### 대안 2. 리스너에 try/catch만 추가

`NotificationEventListener.handleNotificationEvent`에서 예외를 잡아 로그만 남긴다. 두 줄짜리 수정.

- **효과**: 문제 A는 사라진다(예외가 더 이상 전파되지 않으므로).
- **기각 사유**: 문제 B는 그대로 남는다 — 발행이 실패한 알림은 재시도 없이 영원히 유실된다.

### 대안 3. RabbitTemplate 자체 재시도 (`RetryTemplate`)

`rabbitTemplate.setRetryTemplate(...)`로 발행 실패 시 지수 백오프로 재시도하게 설정한다. 설정만으로 되는 가장 가벼운 방법.

- **한계**: 재시도가 **요청을 처리하던 프로세스가 살아있는 동안만** 일어난다. 재시도 도중 그 프로세스가 죽으면(배포로 컨테이너가 교체되거나, OOM으로 강제 종료되거나) 재시도 자체가 함께 사라진다. 또한 "몇 번, 얼마나 기다릴지"를 미리 추측해야 하고, 그 추측이 틀리면(브로커가 재시도 예산보다 오래 죽어 있으면) 결국 유실로 되돌아간다.
- **기각 사유**: ForDay 현재 트래픽 규모(프로세스 1개, 소비자 1개)에서는 이 방식만으로도 실질적으로 충분했을 가능성이 높다. 그럼에도 기각한 이유는 §4에 별도로 정리한다.

### 대안 4. Redis 큐로 우회

RabbitMQ 발행 실패 시 Redis 리스트에 넣고(`ReactionScheduler`가 이미 쓰는 방식) 나중에 드레인한다.

- **기각 사유**: Outbox처럼 보이지만 핵심을 놓친다. "DB에 알림 저장"과 "Redis에 넣기"가 여전히 서로 다른 두 시스템이라, 그 사이에 프로세스가 죽으면 똑같이 유실된다. 원자성 문제 자체가 풀리지 않는다.

### 대안 5. Outbox 패턴 — **채택**

`Notification` 저장과 "발행해야 한다"는 사실(`NotificationOutbox`)을 **같은 DB, 같은 트랜잭션**에 원자적으로 커밋한다. 실제 RabbitMQ 발행은 별도 스케줄러(`NotificationOutboxRelay`)가 주기적으로 PENDING 행을 읽어 처리한다.

```
[reactToRecord 트랜잭션]
  → Notification 저장 + NotificationOutbox 저장 (원자적, 같은 트랜잭션)
  → 커밋. 요청 스레드는 여기서 끝 — RabbitMQ 호출 없음.

[NotificationOutboxRelay, 1초마다]
  → PENDING 행 조회 → 행 잠금 → RabbitMQ 발행 → 성공 시 PUBLISHED
     실패 시 PENDING 유지(다음 주기 재시도) + 최초 실패 시각 기록
```

### 대안 6. CDC 기반 Outbox (Debezium 등)

폴링 대신 MySQL binlog를 실시간으로 읽어 발행하는, Outbox의 더 정교한 버전.

- **기각 사유**: 별도 인프라(Debezium 서버 등)가 필요해 단일 EC2 1인 프로젝트 규모에 과하다.

---

## 3. 결정 — Outbox를 선택한 근거

**핵심 차이는 "재시도 횟수"가 아니라 "보장의 종류"다.**

| | 대안 3 (RetryTemplate) | 대안 5 (Outbox) |
| --- | --- | --- |
| 막는 것 | 프로세스가 살아있는 동안의 일시적 장애 | 그 사실이 DB에 커밋된 이상, 프로세스가 죽어도 살아남음 |
| 못 막는 것 | 재시도 도중 프로세스가 죽으면(배포, OOM, 재부팅) 재시도 자체가 증발 | 없음 — 릴레이는 원 요청 프로세스의 생사와 무관하게 동작 |
| 필요한 설계 | "몇 번, 얼마나 기다릴지" 미리 추측 | 추측 불필요 — 될 때까지 재시도, 상한 없음 |

> 단일 프로세스 재시도(RetryTemplate)는 요청을 처리하던 프로세스가 살아있는 동안의 일시적 장애까지만 막고, 그 프로세스가 재시도 도중 죽으면(배포·재부팅) 재시도 자체가 함께 사라진다. Outbox는 "발행해야 한다"는 사실 자체를 DB 트랜잭션에 원자적으로 커밋해, 프로세스 생사와 무관하게 살아남는 더 강한 보장을 한다. ForDay 현재 규모에서는 전자로도 실질적으로 충분할 수 있으나, 서비스가 여러 개로 쪼개지고 이벤트를 여러 소비자가 구독하기 시작하는 순간부터 후자가 표준이 되는 지점을 직접 구현하며 확인했다.

**정직하게 인정할 부분**: 지금 ForDay 트래픽 규모에서는 대안 3으로도 사실 충분했을 가능성이 크다. RabbitMQ가 "재시도 도중에 프로세스까지 같이 죽는" 시나리오는 흔치 않다. Outbox를 택한 건 "지금 당장 필요해서"가 아니라, 서비스가 여러 개로 쪼개지고 하나의 이벤트를 여러 소비자가 구독하기 시작하는 순간부터 — 즉 요청을 처리하던 그 프로세스가 재시도까지 책임진다는 전제 자체가 무너지는 순간부터 — Outbox(또는 그 CDC 버전)가 선택이 아니라 표준이 되는 지점을 **미리 실제로 구현하며 트레이드오프까지 확인**해보기 위해서다.

**문제 A(성공했는데 500)는 Outbox 도입의 부수 효과로 함께 사라진다.** RabbitMQ 호출이 요청 스레드에서 완전히 빠져나가고 `NotificationEventListener` 자체가 삭제되므로, 전파될 예외가 애초에 없다.

---

## 4. 설계

### 4.1 쓰기 경로

`NotificationService#processReactionNotification`이 `ReactionNotification`과 `NotificationOutbox`(payload = `NotificationEventDto`의 JSON)를 같은 트랜잭션 안에서 저장한다. `ApplicationEventPublisher`/`NotificationEventListener`는 완전히 제거했다 — RabbitMQ 호출이 더 이상 요청 스레드에서 일어나지 않으므로 AFTER_COMMIT 이벤트 메커니즘 자체가 불필요해졌다.

### 4.2 릴레이

`ReactionScheduler`(Redis 큐 배수)와 같은 성격의 "신뢰 가능한 큐 폴링" 패턴을 SQL 테이블에 다시 적용했다.

- `NotificationOutboxRelay`(`@Scheduled(fixedDelay = 1000)`)가 PENDING 행 id 목록을 조회하고, 건별로 `NotificationOutboxItemPublisher.publish(id)`를 호출한다.
- `publish`는 `@Transactional`이며 `SELECT ... FOR UPDATE`로 행을 잠근 뒤 발행한다. **예외를 다시 던지지 않는다** — `@Transactional` 롤백에 기대면 "최초 실패 시각" 기록 자체가 롤백되어 재시도마다 매번 "처음 실패한 것"처럼 보이기 때문에, catch 안에서 상태를 명시적으로 갱신하고 정상 커밋시킨다.
- 자기 자신을 호출하는 구조(`relay()` → `relayOne()`)로 짜면 Spring AOP 프록시를 거치지 않아 `@Transactional`이 적용되지 않는다(`ReactionIndividualSaveService`가 같은 이유로 분리되어 있는 것과 동일한 문제) — 그래서 트랜잭션이 필요한 단위를 별도 빈(`NotificationOutboxItemPublisher`)으로 뺐다.

### 4.3 블루-그린 전환 구간의 중복 발행 — 별도 문제

"발행 실패"와는 무관한, Outbox 특유의 새 문제가 하나 있다. 블루-그린 배포는 신·구 컨테이너가 헬스체크(최대 100초) 동안 동시에 떠 있는다. 이 구간에는 **RabbitMQ 자체는 멀쩡하지만**, 두 인스턴스의 `NotificationOutboxRelay`가 같은 outbox 테이블을 동시에 폴링해 같은 PENDING 행을 동시에 발행할 위험이 있다 — 사용자가 같은 푸시를 두 번 받을 수 있다.

`NotificationOutboxRepository.findByIdForUpdate`의 비관적 락(`SELECT ... FOR UPDATE`)으로 막는다. 한쪽이 행을 잠그면 다른 쪽은 대기했다가, 이미 `PUBLISHED`로 바뀐 걸 확인하고 스킵한다. 이 락은 "발행 재시도"가 아니라 **"두 인스턴스가 동시에 같은 행을 처리하지 못하게" 하는 용도**라는 점을 명확히 해둔다.

### 4.4 운영 알림 — Discord 웹훅

발행이 **최초로** 실패하는 순간과, 실패했던 행이 **나중에 성공(복구)**하는 순간에만 Discord로 알린다(`DiscordAlertPort` → `DiscordWebhookAdapter`). 재시도할 때마다 반복 알리지 않는다 — 브로커가 잠깐 끊기면 대기 중이던 알림 여러 건이 동시에 실패하는데, 매 재시도마다 보내면 Discord가 도배된다.

건(row)별 edge-trigger 방식을 택했다: `NotificationOutbox.failedAt`(최초 실패 시각, null이면 실패한 적 없음)으로 판단한다. 장애 전체를 하나의 사건으로 묶는 전역 상태(예: 전체 outbox 헬스를 나타내는 싱글턴 행)를 쓰면 "장애 1번당 알림 1번"으로 더 깔끔해지지만, 별도의 전역 상태 관리가 필요해 구현이 한 단계 더 복잡해진다 — 지금 규모(1인 개발, 알림 볼륨이 크지 않음)에서는 건별 노이즈가 감수할 만하다고 판단했다.

Discord 호출은 **트랜잭션·행 잠금 밖에서** 한다(`NotificationOutboxRelay.relay()`) — 느린 외부 I/O를 DB 락과 묶어두지 않기 위해서다(#371에서 FCM을 트랜잭션 밖으로 뺀 것과 같은 이유). 전송 실패는 `DiscordWebhookAdapter`가 삼킨다(`PushSenderPort` 컨벤션과 동일) — 운영 알림이 안 나간다고 본 작업이 막히면 안 된다.

### 4.5 받아들인 트레이드오프

- **At-least-once 전달**: 블루-그린 전환 구간에 이론적으로 드물게 중복 발행이 가능하다(§4.3의 락으로 크게 줄였지만 완전히 배제하지는 않는다 — 잠금 획득 직전의 극히 짧은 창은 이론상 남아있다).
- **v1은 무한 재시도만**: `attempts` 카운터·`FAILED` 서킷브레이커를 두지 않았다. 포이즌 필 payload(영구적으로 역직렬화에 실패하는 행) 하나가 무한히 재시도되며 로그만 채울 수 있다 — 실제로 문제가 되면 그때 추가한다.
- **PUBLISHED 행을 삭제하지 않고 보관**한다 — 장애 디버깅 추적성 확보. 정리(cleanup) 배치는 이번 범위 밖.

---

## 5. MSA·대규모 시스템에서의 기대 효과

이 패턴은 ForDay 지금 규모(프로세스 1개, RabbitMQ 소비자 1개)에서는 다소 과할 수 있다는 걸 알면서도, 다음 조건에서 왜 필수가 되는지를 직접 확인하기 위해 구현했다.

- **서비스가 여러 개로 쪼개지는 순간**: 지금은 "요청을 처리하던 프로세스"와 "발행을 재시도할 프로세스"가 같다. MSA로 전환해 알림 발행이 별도 서비스의 책임이 되면, 원 요청 프로세스는 이미 응답을 반환하고 사라진 뒤일 수 있다 — 그 프로세스가 재시도까지 책임진다는 전제 자체가 성립하지 않는다. Outbox는 "발행 책임"을 요청 프로세스의 생명주기에서 완전히 분리한다.
- **하나의 이�트를 여러 소비자가 구독하는 순간**: 지금은 리액션 알림 하나를 `NotificationConsumer` 하나가 소비한다. 서비스가 늘어 같은 이벤트(예: "리액션 등록됨")를 정산·랭킹·알림 등 여러 도메인이 각자 구독해야 하면, "발행 자체의 신뢰성"이 그 모든 소비자에게 영향을 준다 — 발행 단계의 원자성 보장이 갖는 무게가 커진다.
- **응답 시간과 알림 시스템 장애의 완전한 분리**: Outbox 도입 후, 알림 발행 경로(RabbitMQ, FCM)가 통째로 죽어 있어도 리액션 등록 자체의 응답 시간·성공 여부는 전혀 영향받지 않는다. "쓰기 경로"와 "알림 발행 경로"가 서로 다른 장애 도메인이 됐다 — 커머스 규모 시스템에서 결제·주문 같은 핵심 쓰기 경로가 알림·로깅 같은 부가 시스템의 장애에 전염되지 않아야 하는 것과 같은 이유다.

---

## 6. 검증

- `NotificationTransactionalPublishTest` — 롤백 시 `Notification`·`NotificationOutbox` 모두 생기지 않음, 커밋 시 outbox가 `PENDING`으로 정확히 1건 생성됨을 검증.
- `NotificationOutboxRelayTest` — PENDING 발행 성공(`PUBLISHED` 전환), 발행 실패 시 최초 1회만 Discord 알림 + `PENDING` 유지, 실패 후 복구 시 복구 알림 1회, 이미 `PUBLISHED`인 행 재처리 방지(블루-그린 중복 발행 시나리오의 최소 검증).
- `ArchitectureTest`(S5) — `@Transactional` 메서드가 `PushSenderPort`를 직접 호출하지 않는다는 규칙은 이번 변경과 무관하게 계속 통과(`NotificationOutboxItemPublisher`가 호출하는 건 `RabbitTemplate`이지 `PushSenderPort`가 아니다).

## 7. 관련 문서

- [`docs/adr/0001-incremental-hexagonal-architecture.md`](./0001-incremental-hexagonal-architecture.md) — 형식 참고
- [`docs/architecture-rules.md`](../architecture-rules.md) §4 S5
