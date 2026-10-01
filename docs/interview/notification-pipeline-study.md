# 알림 파이프라인 면접 학습 자료

> 대상 사례: "알림 발송이 API 응답을 붙잡고, 장애 시 유실·중복되던 문제 — 딱 한 번 보내기까지"
> 근거: `ForDay_Backend` 작업 트리(2026-09-30 기준), `docs/perf/notification-pipeline.md`, `docs/perf/results/`
> 표기: `클래스명:줄번호`. 전체 경로는 아래 "파일 색인" 참고.

---

## 1분 요약

> 반응(좋아요) API가 푸시 알림(FCM)까지 요청 스레드 안에서 동기로 보내고 있었습니다. FCM을 300ms 지연으로 흉내 내고 초당 250건을 걸어 보니 p95가 33.8초까지 올라갔고, 원인은 톰캣 스레드보다 먼저 마른 DB 커넥션 풀(10개)이었습니다. 발송 트랜잭션 안에서 외부 호출을 기다리느라 커넥션을 쥐고 있었기 때문입니다.
>
> 그래서 단계를 밟았습니다. `@Async`로 떼니 응답은 빨라졌지만 큐가 힙에 있어서, 서버를 강제 종료하자 접수된 알림의 47.5%가 사라졌습니다. 보낼 알림을 같은 트랜잭션에서 DB에 남기는 아웃박스로 바꾸자 유실은 0이 됐지만, 발송 후 상태 갱신이 실패하면 같은 알림이 다시 나갔고, 릴레이가 단일 스레드라 초당 3.1건밖에 못 보냈습니다.
>
> 마지막으로 릴레이는 RabbitMQ에 넣기만 하고, 발송은 컨슈머 20개가 병렬로 하도록 옮겼습니다. 아웃박스와 브로커는 둘 다 최소 한 번 전달이라 중복은 없앨 수 없다고 보고, 컨슈머가 알림 ID를 Redis SETNX로 먼저 선점하게 해서 이미 처리한 알림은 건너뛰게 했습니다. 같은 장애를 30% 확률로 넣었을 때 실제로 1,722건이 재발행됐지만 중복 발송은 0건이었고, 발송 처리량은 초당 3.1건에서 약 48건으로 늘었습니다.

---

## 0. 면접 전에 반드시 처리하거나 알고 갈 것

코드와 문서를 대조하다 찾은 사실들이다. 면접관이 저장소를 열어보거나 꼬리질문을 하면 바로 드러난다.

| # | 사실 | 근거 | 할 일 |
| --- | --- | --- | --- |
| 1 | **4단계의 핵심 코드가 커밋되지 않았다.** `NotificationDeduplicator`, `NotificationEventDispatcher`, `RabbitNotificationEventDispatcher`, 측정 코드 전부 `??`(untracked), `NotificationConsumer`·`NotificationOutbox`(LONGTEXT 수정)·`NotificationOutboxItemPublisher`는 `M`(수정만 됨) | `git status` | 커밋·푸시 전에는 "코드로 보여달라"에 답할 수 없다. 운영 반영 여부도 정리해 둘 것 |
| 2 | **컨슈머 동시성 20은 `measure` 프로파일에만 있다.** `application.yml`·`application-prod.yml`에는 listener 설정이 전혀 없다 → 운영은 기본값(1) | `application-measure.yml:15-16`, `grep` 결과 | 포트폴리오는 "20으로 올렸다"고 쓴다. 운영에도 적용할지, "측정 조건"이라고 밝힐지 정할 것 |
| 3 | **RabbitMQ ack·재시도·DLQ·publisher confirm 설정이 코드와 설정 어디에도 없다.** 전부 Spring AMQP 기본값으로 돈다 | `RabbitMqConfig.java` 전체, yml `grep` 결과 | 3부의 경계 사례에서 반드시 질문받는다 |
| 4 | **발송 실패 시 `release`는 사실상 실행되지 않는다.** `PushSenderPort` 계약이 "실패를 예외로 올리지 않는다"이고, 컨슈머도 토큰마다 예외를 삼킨다 → 바깥 `catch`에 도달할 경로가 없다 | `PushSenderPort:11-14`, `FcmTokenService:45-53`, `NotificationConsumer:44-59` | "실패하면 되돌려서 재시도된다"라고 말하면 틀린다. 3부 참고 |
| 5 | **2단계 원본 측정 파일이 덮어써졌다.** `docs/perf/results/stage2-async-summary.json`은 이후 반응 API 실험의 결과다(카운터 이름 `async_*`, p95 0.63s). 알림 2단계의 9.56s는 `notification-pipeline.md`의 표에만 남아 있다 | 파일 내용 확인 | 캡처 이미지를 증거로 보관할 것 |
| 6 | **`notification-pipeline.md`의 "4단계" 서술이 `(측정 중)`으로 비어 있다** | `notification-pipeline.md` 4단계 절 | 표에는 수치가 있으나 서술을 채워둘 것 |
| 7 | **주석 두 곳이 옛 구조를 설명한다.** "정상 경로는 `@TransactionalEventListener(AFTER_COMMIT)` → RabbitMQ"라고 쓰여 있지만, 코드에 `@TransactionalEventListener`는 더 이상 없다. 지금 정상 경로는 아웃박스 릴레이다 | `SyncPushNotificationSender:23-24`, `ReactionService:128` | 주석 수정. 면접관이 코드를 읽다 보면 헷갈린다 |
| 8 | **1·2단계는 운영 이력이 아니라 측정용 재현이다.** 실제 운영 이력은 "AFTER_COMMIT 이벤트로 RabbitMQ 직접 발행 → 아웃박스(#372)"였다 | `NotificationService:100-103`, `git log` | "처음부터 이 네 단계를 밟았나요?"에 정직하게 답할 준비 |

---

## 파일 색인

| 짧은 이름 | 경로 (`src/main/java/com/example/ForDay/` 기준) |
| --- | --- |
| ReactionService | `domain/reaction/service/ReactionService.java` |
| NotificationService | `domain/notification/service/NotificationService.java` |
| SyncPushNotificationSender | `domain/notification/service/SyncPushNotificationSender.java` |
| AsyncPushNotificationSender | `domain/notification/service/AsyncPushNotificationSender.java` |
| AsyncPushDispatcher | `domain/notification/service/AsyncPushDispatcher.java` |
| MeasurePushRouter | `domain/notification/service/MeasurePushRouter.java` |
| NotificationOutbox | `domain/notification/entity/NotificationOutbox.java` |
| OutboxStatus | `domain/notification/type/OutboxStatus.java` |
| NotificationOutboxRepository | `domain/notification/repository/NotificationOutboxRepository.java` |
| NotificationOutboxRelay | `domain/notification/service/NotificationOutboxRelay.java` |
| NotificationOutboxItemPublisher | `domain/notification/service/NotificationOutboxItemPublisher.java` |
| NotificationEventDispatcher | `domain/notification/service/NotificationEventDispatcher.java` |
| RabbitNotificationEventDispatcher | `domain/notification/service/RabbitNotificationEventDispatcher.java` |
| NotificationConsumer | `domain/notification/service/NotificationConsumer.java` |
| NotificationDeduplicator | `domain/notification/service/NotificationDeduplicator.java` |
| RabbitMqConfig | `global/rabbitmq/config/RabbitMqConfig.java` |
| NotificationEventDto | `global/rabbitmq/dto/NotificationEventDto.java` |
| PushSenderPort / PushMessage | `global/port/PushSenderPort.java`, `global/port/PushMessage.java` |
| FcmPushSenderAdapter | `global/firebase/adapter/FcmPushSenderAdapter.java` |
| FcmTokenService | `global/firebase/service/FcmTokenService.java` |
| MeasuringPushSenderAdapter | `global/measure/adapter/MeasuringPushSenderAdapter.java` |
| MeasuringNotificationEventDispatcher | `global/measure/MeasuringNotificationEventDispatcher.java` |
| MeasurementRecorder | `global/measure/MeasurementRecorder.java` |
| MeasurementStatsService | `global/measure/service/MeasurementStatsService.java` |
| MeasureAsyncConfig | `global/measure/config/MeasureAsyncConfig.java` |
| ScheduleConfig | `global/config/schedule/ScheduleConfig.java` |
| application-measure.yml | `src/main/resources/application-measure.yml` |
| ArchitectureTest | `src/test/java/com/example/ForDay/architecture/ArchitectureTest.java` |
| k6 1·2단계 | `scripts/k6/notification-stage1-sync.js` (`/records/{id}/reaction/test`) |
| k6 3·4단계 | `scripts/k6/notification-stage34-outbox-rabbit.js` (`/records/{id}/reaction`) |

---

# 1부. 기초 개념

각 항목: **정의 → 왜 필요한가 → 이 프로젝트에서**

### 1-1. 동기 처리와 비동기 처리

- **정의**: 동기는 호출한 쪽이 결과가 올 때까지 기다린다. 비동기는 작업을 넘기고 바로 다음 일을 한다.
- **왜**: 외부 API(FCM)처럼 느리고 실패할 수 있는 작업을 요청 처리 흐름 안에 두면, 그 지연이 곧 API 응답 지연이 된다.
- **이 프로젝트**: 1단계 `SyncPushNotificationSender:64-72`는 요청 스레드에서 FCM(측정용 300ms)을 부른다. 2단계 `AsyncPushNotificationSender:58`은 `AsyncPushDispatcher`로 넘기고 즉시 반환한다.

### 1-2. 톰캣 스레드 풀

- **정의**: 톰캣은 요청 하나를 스레드 하나가 처음부터 끝까지 처리한다(thread-per-request). 기본 최대 200개.
- **왜**: 스레드가 느린 I/O를 기다리는 동안에도 점유되므로, 동시 요청 수가 스레드 수를 넘으면 뒤 요청은 대기열에서 기다린다.
- **이 프로젝트**: `application-measure.yml:32`에 `max: 200`을 기본값 그대로 명시(재현 근거를 남기려고). 1단계에서 busy 200/200을 관측.

### 1-3. HikariCP 커넥션 풀과 "스레드보다 커넥션이 먼저 마르는" 현상

- **정의**: DB 커넥션을 미리 만들어 두고 빌려 쓰는 풀. Spring Boot 기본 최대 10개.
- **왜**: 커넥션은 트랜잭션이 끝날 때 반납된다. 트랜잭션 안에서 느린 외부 호출을 하면 **일은 안 하는데 커넥션은 쥐고 있는** 상태가 된다.
- **이 프로젝트**: 1단계 호출 경로는 `ReactionService:132-133 @Transactional testReactToRecord` → `:148` → `NotificationService:160-168` → `MeasurePushRouter:35-41` → `SyncPushNotificationSender:64-72`(FCM 호출). **FCM 호출이 바깥 트랜잭션 안에서 일어난다.** 요청 1건이 커넥션을 최소 300ms 쥐니, 커넥션 10개로는 이론상 초당 약 33건이 한계다(10 ÷ 0.3s). 실측 처리량 32.7 rps가 이 값과 맞는다. 스레드는 200개까지 늘어나지만 그중 191개가 커넥션을 기다렸다(`notification-pipeline.md` 1단계).

> 면접 포인트: "스레드 200개가 다 찼다"보다 "스레드 200개 중 191개가 커넥션 10개를 기다렸다"가 진짜 원인이다. 스레드를 늘려도 해결되지 않는다.

### 1-4. Spring `@Async`

- **정의**: 메서드를 별도 스레드 풀(TaskExecutor)에서 실행하게 하는 애노테이션. `@EnableAsync`가 있어야 동작한다.
- **왜**: 호출자를 기다리게 하지 않으려고.
- **동작 원리**: Spring이 빈을 **프록시**로 감싸고, 프록시가 호출을 가로채 실행자에 제출한다. 그래서 **같은 클래스 안에서 자기 메서드를 부르면(self-invocation) 프록시를 거치지 않아 비동기가 되지 않는다.**
- **이 프로젝트**: `MeasureAsyncConfig:18-21`(`@EnableAsync`, measure 전용). self-invocation을 피하려고 `AsyncPushDispatcher`를 별도 빈으로 뺐다(`AsyncPushDispatcher:17-20` 주석).

### 1-5. 기본 실행자 `applicationTaskExecutor`

- **정의**: 실행자 빈을 따로 정의하지 않으면 Spring Boot가 만들어 주는 `ThreadPoolTaskExecutor`. 코어 스레드 8개, **큐 용량 제한 없음**(기본값 — Spring Boot 문서로 확인 권장).
- **왜 위험한가**: 큐가 무제한이면 `@Async` 호출은 항상 즉시 성공한다. 처리 속도보다 유입이 빠르면 작업이 힙에 계속 쌓이고, 프로세스가 죽으면 같이 사라진다. 큐가 꽉 차야 늘어나는 최대 스레드 수도 사실상 쓰이지 않는다.
- **이 프로젝트**: 일부러 전용 실행자를 두지 않았다(`MeasureAsyncConfig:11-16` 주석) — "그냥 `@Async`를 붙이면 무슨 일이 생기는가"를 재현하려고. 스레드 8개 × (1초 ÷ 0.3초) = 초당 약 26.7건이 발송 상한이고, 실측 26건/s가 여기에 붙었다. 부하 종료 시점 큐에 6,890건.

### 1-6. 트랜잭션 경계와 전파

- **정의**: `@Transactional`이 붙은 메서드가 시작될 때 트랜잭션이 열리고 끝날 때 커밋/롤백된다. 기본 전파 `REQUIRED`는 이미 열린 트랜잭션이 있으면 **거기에 참여**한다.
- **왜**: 어떤 작업들이 "같이 성공하거나 같이 실패하는지"가 여기서 정해진다.
- **이 프로젝트**:
  - 3·4단계: `ReactionService:106-107 @Transactional reactToRecord`가 반응 저장, 카운트 upsert, `NotificationService.processReactionNotification`(`:121`)까지 한 트랜잭션으로 묶는다. 그 안에서 아웃박스 행이 저장된다(`NotificationService:135`). 그래서 **반응과 "보내야 할 알림"이 같이 커밋된다.**
  - `findActiveRecordDeviceToken`(`NotificationService:170-171`)은 `readOnly=true`지만 바깥 트랜잭션에 참여하므로 새 트랜잭션이 열리지 않는다(참여한 트랜잭션에서는 바깥 속성이 적용됨 — 확인 권장).
  - MongoDB 저장(`NotificationService:116-122`)은 MySQL 트랜잭션에 묶이지 않는다. 그래서 순서로 정합성을 맞춘다: Mongo 먼저 저장, 실패하면 예외를 던져 MySQL을 롤백(`NotificationService:92-98` 주석). 반대로 Mongo 성공 후 MySQL이 롤백되면 고아 문서가 남는다(허용한 범위).

### 1-7. 커밋 이후 실행 (`@TransactionalEventListener(AFTER_COMMIT)`)

- **정의**: 트랜잭션이 커밋된 뒤에만 이벤트 리스너를 실행한다.
- **왜**: 커밋 전에 외부로 알리면, 트랜잭션이 롤백돼도 알림은 이미 나간다.
- **이 프로젝트**: **현재 코드에는 없다.** 예전에 AFTER_COMMIT으로 RabbitMQ를 직접 호출했으나 (1) 발행 실패 시 재시도 없이 유실되고 (2) 발행 중 예외가 트랜잭션 밖으로 전파돼 이미 커밋된 반응이 클라이언트에는 500으로 보이는 문제가 있어 아웃박스로 바꿨다(`NotificationService:100-103`). 주석 두 곳은 아직 옛 구조를 가리킨다(0장 7번).

### 1-8. 트랜잭셔널 아웃박스 패턴

- **정의**: "외부로 보내야 할 메시지"를 비즈니스 데이터와 **같은 트랜잭션으로 같은 DB에** 저장해 두고, 별도 프로세스가 나중에 꺼내 보낸다.
- **왜**: DB 커밋과 외부 발송을 원자적으로 묶을 방법이 없다(이중 쓰기 문제). 아웃박스는 "커밋됐다면 보낼 일이 반드시 DB에 남아 있다"를 보장한다.
- **이 프로젝트**: `NotificationOutbox`(`:30-98`), 생성은 `NotificationOutbox.pending`(`:74-76`), 상태는 `PENDING`/`PUBLISHED` 두 개뿐(`OutboxStatus:9-12`, 의도적으로 FAILED/시도 횟수 없음).

### 1-9. 릴레이(폴링 퍼블리셔)

- **정의**: 아웃박스 테이블을 주기적으로 조회해 PENDING 행을 외부로 보내고 상태를 바꾸는 작업.
- **이 프로젝트**: `NotificationOutboxRelay:35-48`. 1초마다(`fixedDelay=1000`), 오래된 순으로 100개 id를 가져와(`:29, :37`) 건별로 `NotificationOutboxItemPublisher.publish`를 호출한다. 건별 트랜잭션을 쓰려고 발행 로직을 별도 빈으로 뺐다(self-invocation 회피, `NotificationOutboxItemPublisher:15-18`).
- **스케줄러 스레드**: `@EnableScheduling`만 있고(`ScheduleConfig:6-8`) 풀 크기 설정이 없다 → Spring Boot 기본 스케줄러 스레드는 1개(기본값 — 확인 권장). **다른 `@Scheduled` 작업(`ReactionScheduler` 등)과 같은 스레드를 나눠 쓴다.**

### 1-10. 비관적 락 (`SELECT ... FOR UPDATE`)

- **정의**: 행을 읽는 순간 쓰기 잠금을 건다. 다른 트랜잭션은 같은 행을 잠그려면 기다린다.
- **왜**: 릴레이가 두 인스턴스(블루-그린 전환 구간)에서 동시에 돌 때 같은 행을 둘 다 발행하지 않게.
- **이 프로젝트**: `NotificationOutboxRepository:25-27`(`PESSIMISTIC_WRITE`). 후보 id 조회(`:19-20`)에는 락을 걸지 않고, 건별로 잠근 뒤 `isPublished()`를 다시 확인한다(`NotificationOutboxItemPublisher:37-41`).

### 1-11. 메시지 브로커와 RabbitMQ 기본 구조

- **정의**: 생산자와 소비자 사이에서 메시지를 받아 보관하고 전달하는 서버.
- **구성 요소**:
  - **Exchange**: 메시지를 받아 규칙대로 큐에 나눠 준다. Topic exchange는 routing key 패턴으로 라우팅한다.
  - **Queue**: 메시지가 쌓이는 곳. **durable**이면 브로커 재시작 후에도 큐 정의가 남는다(메시지가 살아남으려면 메시지도 persistent여야 함).
  - **Binding**: exchange와 queue를 routing key로 연결.
- **이 프로젝트**: `RabbitMqConfig:17-19`(durable 큐 `notification.queue`), `:22-24`(TopicExchange), `:27-29`(binding), `:32-34`(Jackson JSON 변환기). 발행은 `RabbitNotificationEventDispatcher:17-20`(`convertAndSend`).

### 1-12. ack/nack, 재배달, prefetch, 컨슈머 동시성

- **ack**: 컨슈머가 "처리 끝"을 브로커에 알림 → 브로커가 메시지를 지운다. ack 전에 연결이 끊기면 브로커는 **다시 배달**한다.
- **Spring AMQP 기본 ack 모드 AUTO**: 리스너 메서드가 정상 반환하면 ack, 예외를 던지면 reject. 기본적으로 reject된 메시지를 **다시 큐에 넣는다**(requeue). 재시도 인터셉터는 기본 비활성. (Spring AMQP 3.x 기본값 — 공식 문서로 확인 권장)
- **prefetch**: 컨슈머 하나가 ack 없이 미리 받아둘 수 있는 메시지 수. Spring AMQP 기본 250(확인 권장).
- **컨슈머 동시성**: 리스너 컨테이너가 띄우는 소비 스레드 수. 기본 1.
- **이 프로젝트**: 리스너는 `NotificationConsumer:24-25`. **ack 모드·requeue·재시도·prefetch를 코드나 설정에서 바꾸지 않았다** → 전부 기본값. 동시성만 `application-measure.yml:15-16`에서 20(measure 전용).

### 1-13. 전달 보장 수준

| 수준 | 의미 | 대가 |
| --- | --- | --- |
| at-most-once | 많아야 한 번. 유실될 수 있지만 중복은 없음 | 유실 |
| at-least-once | 최소 한 번. 유실은 없지만 중복될 수 있음 | 중복 |
| exactly-once | 정확히 한 번 | 분산 시스템 사이(특히 외부 API 호출)에서는 일반적으로 달성 불가 |

- **"exactly-once 효과 = at-least-once 전달 + 멱등 소비자"**: 보내는 쪽은 여러 번 보내도 되게 하고, 받는 쪽이 "이미 처리한 것"을 걸러서 **결과적으로** 한 번만 효과가 나게 만든다.
- **이 프로젝트**: 아웃박스 릴레이와 브로커 재배달이 at-least-once를 만들고(`NotificationDeduplicator:13-16` 주석), 컨슈머의 Redis 선점이 멱등 소비자 역할을 한다. 단, 3부에서 보듯 **좁은 유실 구간이 있어 엄밀한 exactly-once는 아니다.**

### 1-14. 멱등성, Redis SETNX, TTL

- **멱등성**: 같은 요청을 여러 번 해도 결과가 한 번 한 것과 같은 성질.
- **SETNX**(`SET key value NX`): 키가 없을 때만 설정하고 성공 여부를 돌려준다. Redis는 명령을 하나씩 처리하므로 **동시에 여러 클라이언트가 같은 키로 시도해도 하나만 성공한다.**
- **TTL**: 키 만료 시간. 멱등성 키가 무한히 쌓이지 않게 한다.
- **이 프로젝트**: `NotificationDeduplicator:41-49`에서 `setIfAbsent(KEY_PREFIX + id, "1", TTL)` — SET NX와 만료를 한 번에 건다(원자적). 키 `notification:dispatched:{notificationId}`(`:29`), TTL 24시간(`:32`).

### 1-15. 헥사고날 아키텍처의 포트와 어댑터

- **정의**: 도메인은 "무엇이 필요한지"만 인터페이스(포트)로 정의하고, 실제 구현(어댑터)은 바깥에 둔다.
- **왜**: 외부 기술을 바꾸거나, 테스트·측정용 구현을 끼우기 쉽다.
- **이 프로젝트**: 포트 `PushSenderPort:8-15`, 메시지 `PushMessage:12-18`. 운영 어댑터 `FcmPushSenderAdapter:13-28`, 측정 어댑터 `MeasuringPushSenderAdapter:26-57`(`@Primary @Profile("measure")`) — 300ms 대기 후 Redis에 발송 기록. **발송 코드를 한 줄도 바꾸지 않고** 실제 FCM 대신 지연만 흉내 내는 구현을 끼울 수 있었던 이유가 이 포트다. 발행 방식도 포트화했다: `NotificationEventDispatcher:15-17` → 운영 `RabbitNotificationEventDispatcher`, 측정 `MeasuringNotificationEventDispatcher`(direct/rabbit 전환 + 장애 주입).

### 1-16. p50/p95/p99와 처리량

- **pXX**: 응답 시간을 빠른 순으로 줄 세웠을 때 XX% 위치의 값. p95 = 요청의 95%가 이 시간 안에 끝남.
- **왜 평균이 아닌가**: 평균은 소수의 매우 느린 요청을 가린다. 사용자가 실제로 겪는 최악에 가까운 경험은 꼬리(p95/p99)에서 드러난다.
- **처리량**: 단위 시간당 끝난 요청 수(rps) 또는 발송 수(건/s). 응답 시간과는 다른 지표다 — 4단계에서 p95는 그대로인데 발송 처리량은 15배 늘었다.

### 1-17. k6 `constant-arrival-rate`와 VU 고정 방식

- **VU 고정(closed model)**: 가상 사용자 N명이 "요청 → 응답 → 다음 요청"을 반복한다. 서버가 느려지면 요청 속도도 같이 떨어져 **과부하가 가려진다.**
- **도착률 고정(open model)**: 서버 응답과 상관없이 초당 N건을 넣는다. 서버가 못 따라가면 VU가 늘고, VU 한도에 닿으면 **보내지도 못한 요청(`dropped_iterations`)**이 생긴다.
- **이 프로젝트**: 초당 250건, 60초, VU 200~1000(`notification-stage34-outbox-rabbit.js:22-27`, 1·2단계 스크립트도 동일). 1단계의 **12,139건은 알림이 아니라 k6가 서버에 보내지도 못한 요청 수**(`stage1-sync-summary.json`의 `dropped_iterations`)다.

---

# 2부. 코드 흐름 따라가기

공통 진입: k6 → `POST /records/{id}/reaction/test`(1·2단계) 또는 `POST /records/{id}/reaction`(3·4단계).

### 2-1. 1단계 — 동기

| 순서 | 스레드 | 코드 | 트랜잭션 |
| --- | --- | --- | --- |
| 1 | 톰캣 요청 스레드 | `ReactionService:133` `testReactToRecord` | **시작**(커넥션 획득) |
| 2 | 같음 | 반응 저장, 카운트 upsert | 안 |
| 3 | 같음 | `NotificationService:160-168` → `MeasurePushRouter:40` | 안 |
| 4 | 같음 | `SyncPushNotificationSender:45-48` 알림을 MySQL(JPA)에 저장 | 안 |
| 5 | 같음 | `:58` 발송 확정 카운트(Redis) | 안 |
| 6 | 같음 | `:64-72` 토큰마다 `PushSenderPort.send` → 300ms 대기 | **안(커넥션 쥔 채 대기)** |
| 7 | 같음 | 메서드 종료 | **커밋**(커넥션 반납) |

- **프로세스가 죽으면**: 커밋 전이면 전부 롤백, 알림은 일부 나갔을 수 있다. 커밋 후라면 이미 보냈다.
- **중복 가능 지점**: 없음. 대신 **커밋 전에 발송**하므로 롤백돼도 알림이 나가는 반대 문제가 있다.
- **주의**: 1·2단계는 알림을 MySQL(JPA `ReactionNotification`)에 저장하고, 3·4단계는 MongoDB(`NotificationDocumentService`)에 저장한다. 요청 경로의 비용이 단계마다 완전히 같지는 않다.

### 2-2. 2단계 — `@Async`

| 순서 | 스레드 | 코드 | 트랜잭션 |
| --- | --- | --- | --- |
| 1~4 | 요청 스레드 | 1단계와 동일, 저장은 `AsyncPushNotificationSender:44-47` | 안 |
| 5 | 요청 스레드 | `:55` 발송 확정 카운트 — **디스패치 전에** 올린다(큐에서 사라진 것도 분모에 들어가야 유실을 셀 수 있음, `:25-27` 주석) | 안 |
| 6 | 요청 스레드 | `:58` `asyncPushDispatcher.dispatch(...)` → 실행자 큐에 **제출만 하고 반환** | 안 |
| 7 | 요청 스레드 | 메서드 종료 | 커밋 |
| 8 | `task-N` 스레드(8개) | `AsyncPushDispatcher:33-41` 토큰마다 발송 | 없음 |

- **프로세스가 죽으면**: 커밋된 알림 행은 남지만, 실행자 큐(힙)에 있던 발송 작업은 사라진다. 재시작 후 아무도 다시 보내지 않는다 → **영구 유실**(8,552건 중 4,064건).
- **중복 가능 지점**: 없음.
- **숨은 문제**: 6번이 커밋 **전**에 일어난다. 트랜잭션이 이후에 롤백돼도 발송 작업은 이미 큐에 들어가 있다. 실무라면 커밋 이후에 제출해야 한다.

### 2-3. 3단계 — 아웃박스(브로커 없음, `measure.outbox.target=direct`)

**요청 경로**

| 순서 | 스레드 | 코드 | 트랜잭션 |
| --- | --- | --- | --- |
| 1 | 요청 스레드 | `ReactionService:107` `reactToRecord` | 시작 |
| 2 | 같음 | 반응 저장, 카운트 upsert, 랭킹 | 안 |
| 3 | 같음 | `NotificationService:116-122` 알림을 MongoDB에 저장(트랜잭션 밖) | Mongo는 별도 |
| 4 | 같음 | `:124` 토큰 조회, `:135` **아웃박스 PENDING 행 저장** | 안 |
| 5 | 같음 | 종료 | **커밋** — 반응과 PENDING 행이 함께 확정 |

**발송 경로**

| 순서 | 스레드 | 코드 | 트랜잭션 |
| --- | --- | --- | --- |
| 6 | 스케줄러 스레드(1개) | `NotificationOutboxRelay:36-37` PENDING id 100개 조회 | 없음 |
| 7 | 같음 | 건마다 `NotificationOutboxItemPublisher:36` | **건별 시작** |
| 8 | 같음 | `:37` `findByIdForUpdate` 행 잠금, 이미 PUBLISHED면 NOOP | 안 |
| 9 | 같음 | `:44-45` payload 역직렬화 → `MeasuringNotificationEventDispatcher:53-54` → `sendDirectly`(`:67-75`) → `PushSenderPort.send` 300ms | **안(행 락+커넥션 쥔 채 대기)** |
| 10 | 같음 | (주입) `:61-64` 30% 확률로 발송 **뒤** 예외 | 안 |
| 11a | 같음 | 정상: `:48` `markPublished()` | 커밋 |
| 11b | 같음 | 예외: `:50-56`에서 **삼키고** `markFailed()`(최초 실패 시각만 기록) → 상태는 PENDING 그대로 | **커밋** |

- **프로세스가 죽으면**: 커밋된 PENDING 행은 DB에 남는다 → 재시작 후 릴레이가 이어서 보낸다(kill 후 PENDING 6,646건 생존, 재기동 45초 동안 135건 발송 확인). **유실 0.**
- **중복 가능 지점**: 9번(발송 성공)과 11번(상태 갱신) 사이. 11b처럼 상태가 PENDING으로 남거나, 9번 직후 프로세스가 죽으면 다음 주기에 같은 행을 다시 보낸다 → 28건 중복.
- **처리량이 3.1건/s인 이유**: 스케줄러 스레드 1개가 한 건씩 300ms씩 직렬로 보낸다 → 이론 상한 3.3건/s. 6,969건을 다 보내려면 약 36분.
- **ArchUnit S5가 못 잡는 것**: 9번은 `@Transactional` 메서드 안에서 FCM을 부른다. 하지만 S5는 **직접 호출만** 본다(`ArchitectureTest:276-293`) — `publish` → `NotificationEventDispatcher.dispatch` → `PushSenderPort.send`처럼 한 단계만 돌아가도 걸리지 않는다. 운영 경로(`RabbitNotificationEventDispatcher`)는 브로커 발행이라 문제가 작지만, 측정용 direct 경로는 실제로 트랜잭션 안 외부 호출이다.

### 2-4. 4단계 — RabbitMQ + 멱등

요청 경로는 3단계와 **완전히 같다.** 달라지는 것은 발송 경로다.

| 순서 | 스레드 | 코드 | 트랜잭션 |
| --- | --- | --- | --- |
| 6~8 | 스케줄러 스레드 | 3단계와 동일 | 건별 |
| 9 | 같음 | `MeasuringNotificationEventDispatcher:56` → `RabbitNotificationEventDispatcher:18-19` `convertAndSend` (브로커로 네트워크 전송, 빠름) | 안(행 락 쥔 채) |
| 10 | 같음 | (주입) 30% 확률로 발행 **뒤** 예외 | 안 |
| 11 | 같음 | 정상 PUBLISHED / 예외면 PENDING 유지 → 다음 주기 **재발행** | 커밋 |
| 12 | 컨슈머 스레드(20개) | `NotificationConsumer:24-25` 메시지 수신(JSON → DTO) | 없음 |
| 13 | 같음 | `:30-33` 토큰 없으면 종료(ack) | |
| 14 | 같음 | `:37-40` `claim(notificationId)` — SETNX 실패면 **건너뛰고 종료(ack)** | |
| 15 | 같음 | `:44-54` 토큰마다 발송(300ms), 실패는 로그만 | |
| 16 | 같음 | 정상 반환 → 컨테이너가 **ack** | |

- **프로세스가 죽으면**: 커밋된 PENDING 행은 남음. 브로커에 들어간 메시지는 durable 큐 + (기본) persistent 메시지면 브로커 재시작도 견딘다(확인 권장). 컨슈머가 ack 전에 죽으면 브로커가 재배달한다.
- **중복 가능 지점과 차단**: 재발행(11) 또는 재배달이 일어나도 14번에서 이미 선점된 키에 막힌다. 실측: 1,722건이 재발행됐고 중복 발송은 0건.
- **처리량**: 컨슈머 20개 × 1초/0.3초 = 이론 약 66건/s, 실측 약 48건/s(차이의 원인은 따로 계측하지 않았다 — 단일 스케줄러 스레드의 발행 속도, 30% 재발행 부하, prefetch 분배 등이 후보).

### 2-5. 4단계 전체 시퀀스

```
Client        API(요청 스레드)        MySQL           MongoDB     Relay(스케줄러 1)        RabbitMQ            Consumer(x20)          Redis              FCM(측정: 300ms)
  |  POST /reaction  |                   |                |               |                      |                     |                    |                   |
  |----------------->| BEGIN TX -------->|                |               |                      |                     |                    |                   |
  |                  | 반응 저장/카운트 -->|                |               |                      |                     |                    |                   |
  |                  | 알림 문서 저장 ----------------------->|               |                      |                     |                    |                   |
  |                  | outbox PENDING -->|                |               |                      |                     |                    |                   |
  |                  | COMMIT ---------->|                |               |                      |                     |                    |                   |
  |<-----------------| 200               |                |               |                      |                     |                    |                   |
  |                  |                   |<- PENDING id 100개 (1초마다) ---|                      |                     |                    |                   |
  |                  |                   |<- SELECT FOR UPDATE (건별 TX) --|                      |                     |                    |                   |
  |                  |                   |                |               |-- convertAndSend --->|                     |                    |                   |
  |                  |                   |<- PUBLISHED 갱신 + COMMIT ------|  (실패 시 PENDING 유지 → 다음 주기 재발행)   |                    |                   |
  |                  |                   |                |               |                      |-- deliver --------->|                    |                   |
  |                  |                   |                |               |                      |                     |-- SET NX EX 24h -->|                   |
  |                  |                   |                |               |                      |                     |<-- true / false ---|                   |
  |                  |                   |                |               |                      |                     |  false면 건너뜀     |                   |
  |                  |                   |                |               |                      |                     |-- send (토큰마다) ---------------------->|
  |                  |                   |                |               |                      |<-- ack (정상 반환) --|                    |                   |
```

---

# 3부. 고려한 상황과 경계 사례

### 3-1. 릴레이가 발송은 성공했는데 PUBLISHED 갱신이 실패하면?

- **코드 경로**: 발행(`NotificationOutboxItemPublisher:45`) 뒤 예외가 나면 `:50-56`에서 삼키고 `markFailed()`만 한 뒤 **커밋**한다. 상태는 PENDING으로 남는다. 커밋 자체가 실패하거나 커밋 직전에 프로세스가 죽는 경우도 결과는 같다(PENDING 유지).
- **결과**: 다음 주기(1초 후)에 같은 행을 다시 발행한다. 3단계에서는 이게 그대로 중복 발송(28건), 4단계에서는 중복 메시지가 되지만 컨슈머 선점에서 걸러진다(1,722건 재발행, 중복 0).
- **측정 방법**: 이 상황은 `MeasuringNotificationEventDispatcher:61-64`가 발송 **뒤에** 예외를 던져 재현했다. 앞에서 던지면 단순 발송 실패라 아무 문제도 드러나지 않는다(`:59-60` 주석).
- **현재 코드의 한계**: 아웃박스 쪽에서는 이 틈을 없앨 수 없다(구조적). 재시도 횟수 제한도 없어 영구 실패 행은 무한 재시도된다.
- **개선하려면**: 틈은 받는 쪽 멱등성으로 흡수(현재 방식). 추가로 시도 횟수 컬럼 + 한도 초과 시 FAILED 상태로 격리.

### 3-2. 컨슈머가 Redis claim 후 발송 전에 죽으면?

- **코드 경로**: `claim`이 성공하면 키가 24시간 TTL로 남는다(`NotificationDeduplicator:46-47`). 발송 전에 프로세스가 죽으면 `release`(`NotificationConsumer:57`)는 실행되지 않는다. 메시지는 ack되지 않았으므로 브로커가 다른 컨슈머에게 재배달한다. 재배달된 메시지는 `claim`이 false → **건너뛰고 ack.**
- **결과**: **그 알림은 유실된다.** 24시간 동안 어떤 재배달도 이 키에 막힌다. 선점을 "발송 전"에 하는 설계가 이 구간을 중복 대신 유실로 바꾼 것이다.
- **정직한 평가**: 창은 좁다(claim과 FCM 호출 사이 수 ms~수백 ms). 하지만 존재한다. 4단계에서는 **컨슈머 강제 종료 실험을 하지 않았으므로** "유실 0"은 이 상황을 검증한 결과가 아니다.
- **현재 코드의 한계**: 선점 상태가 "처리 중"과 "처리 완료"를 구분하지 않는다.
- **개선하려면**: 2단계 상태 — claim 시 `PROCESSING`(짧은 TTL, 예: 60초), 발송 후 `DONE`(24시간)으로 덮어쓴다. 죽으면 PROCESSING이 곧 만료돼 재배달이 처리된다(그 대신 아주 드물게 중복 가능).

### 3-3. 컨슈머가 발송 후 ack 전에 죽으면?

- **코드 경로**: 발송은 끝났고 키도 남아 있다. ack가 안 갔으니 브로커가 재배달 → `claim` false → 건너뛰고 ack.
- **결과**: **중복 없음.** 멱등성 게이트가 설계대로 동작하는 대표 상황이다.
- **현재 코드의 한계**: 없음(24시간 이후 재배달이면 다시 보낸다 — 현실적으로 드묾).
- **개선하려면**: 필요 없음. TTL을 브로커 최대 재배달 지연보다 길게 유지.

### 3-4. 발송 중 예외가 나면 release 후 재시도는 누가, 몇 번, 어떻게? 무한 반복? DLQ?

- **코드 경로 (실제)**:
  1. `PushSenderPort` 계약: "전송 실패는 예외로 올리지 않고 삼킨다"(`PushSenderPort:11-14`). 운영 어댑터도 `FirebaseMessagingException`을 잡아 로그만 남긴다(`FcmTokenService:45-53`).
  2. 컨슈머도 토큰마다 `catch (Exception e)`로 삼킨다(`NotificationConsumer:50-53`).
  3. 따라서 바깥 `catch`(`:55-59`)의 `release`와 재던지기에 **도달할 경로가 사실상 없다.**
- **결과**: FCM 발송이 실패해도 메서드는 정상 반환 → ack → 메시지 삭제. 키는 남아 있다. **재시도 없음, 그 알림은 유실.** `NotificationDeduplicator:21-22`와 컨슈머 주석이 말하는 "실패하면 되돌린다"는 의도일 뿐 현재 경로에서는 일어나지 않는다.
- **만약 예외가 바깥으로 나간다면**(예: `claim`에서 Redis 예외): Spring AMQP 기본(AUTO ack, requeue=true, 재시도 없음)으로 reject 후 **즉시 다시 큐에 넣는다** → 원인이 사라질 때까지 무한 재배달 루프. 재시도 횟수 제한·백오프·DLQ는 **설정되어 있지 않다**(기본값 — 확인 권장).
- **현재 코드의 한계**: 실패를 구분하지 못해 일시 장애(FCM 5xx, 할당량)와 영구 실패(잘못된 토큰)를 똑같이 버린다. 반대로 예외가 새어 나가는 경우엔 무한 루프.
- **개선하려면**: 포트가 결과(성공/재시도 가능 실패/영구 실패)를 돌려주게 하고, 재시도 가능 실패면 `release` 후 예외로 nack. 컨테이너에 재시도 인터셉터(최대 N회, 지수 백오프) + DLX/DLQ 설정. 영구 실패(`UNREGISTERED` 등)는 토큰 삭제.

### 3-5. Redis가 죽으면 멱등성 게이트는?

- **코드 경로**: `setIfAbsent`가 연결 예외를 던진다(`NotificationDeduplicator:46`). 이 호출은 `try` 바깥이라(`NotificationConsumer:37`) 예외가 리스너 밖으로 나간다 → 기본 설정상 reject + requeue.
- **결과**: 발송은 일어나지 않는다(**fail-closed** — 중복도 유실도 없다). 대신 Redis가 돌아올 때까지 같은 메시지가 계속 재배달되며 CPU와 로그를 소모한다. 큐 적체.
- **현재 코드의 한계**: 백오프 없는 재배달 루프. Redis 재시작 시 영속화(AOF/RDB) 설정에 따라 키가 사라지면 그 뒤 재배달은 **중복 발송**될 수 있다.
- **개선하려면**: 재시도 백오프 설정. Redis 장애 시 정책 결정(안 보내고 기다릴지, 중복을 감수하고 보낼지). 더 강한 보장이 필요하면 DB 기반 처리 이력으로 이중화.

### 3-6. 같은 기록에 여러 사용자가 반응하면 notificationId와 Redis 키는?

- **코드 경로**: 반응 한 건마다 `processReactionNotification`이 알림 문서를 새로 저장하고, ID는 Mongo 시퀀스(`NotificationDocumentService:29` `generateSequence`)로 새로 발급된다. 아웃박스 행·메시지·Redis 키 모두 이 ID 기준이다(`NotificationService:117-135`, `NotificationDeduplicator:47`).
- **결과**: A의 반응 → `notification:dispatched:501`, B의 반응 → `:502`. 서로 독립이라 둘 다 발송된다. 게이트가 막는 건 **같은 알림의 재배달**뿐이다.
- **현재 코드의 한계**: 같은 알림에 토큰이 여러 개(기기 여러 대)면 토큰 단위가 아니라 알림 단위로 선점한다 → 일부 토큰만 실패해도 재시도 없음(3-4와 같은 문제).
- **개선하려면**: 필요하면 키를 `notificationId:token`으로 세분화.

### 3-7. 컨슈머 동시성 20에서 같은 notificationId 메시지 두 개가 동시에 도착하면?

- **코드 경로**: 두 스레드가 거의 동시에 `setIfAbsent`를 호출한다. Redis는 명령을 순서대로 처리하므로 **하나만 true**, 다른 하나는 false.
- **결과**: 한 번만 발송. 동시성을 얼마나 올려도 이 부분은 안전하다.
- **현재 코드의 한계**: 없음(SETNX + TTL이 한 명령이라 "SET은 됐는데 만료가 안 걸린" 상태도 생기지 않는다).
- **개선하려면**: 해당 없음.

### 3-8. 릴레이가 여러 인스턴스에서 동시에 돌면 같은 행을 두 번 집을 수 있나?

- **코드 경로**: 후보 id 조회는 잠그지 않는다(`NotificationOutboxRepository:17-20`). 건별로 `PESSIMISTIC_WRITE`로 잠그고, 잠금을 얻은 뒤 `isPublished()`를 다시 본다(`NotificationOutboxItemPublisher:37-41`).
- **결과**: 정상 발행된 행을 두 인스턴스가 둘 다 발행하지는 않는다(한쪽이 대기 후 PUBLISHED를 보고 NOOP). 다만 3-1처럼 상태 갱신이 실패한 행은 어느 인스턴스든 다시 발행한다(구조적 중복 — 컨슈머가 흡수).
- **현재 코드의 한계**: 두 인스턴스가 **같은 100개를 같은 순서로** 집기 때문에 서로 행 락을 기다리며 직렬화된다. 병렬 효과가 없고, 대기하는 동안 커넥션을 쥔다. 또 영구 실패 행은 PENDING으로 남아 가장 오래된 순서의 앞자리를 계속 차지한다 — 이런 행이 100개를 넘으면 **새 알림이 영영 발행되지 않는다**(head-of-line blocking). `status` 컬럼에 인덱스도 없고 PUBLISHED 행을 지우지 않아 테이블이 커질수록 조회가 느려진다(`NotificationOutbox:24-27` 주석, `@Table`에 인덱스 없음).
- **개선하려면**: `FOR UPDATE SKIP LOCKED`로 서로 다른 행을 나눠 집기, `(status, id)` 인덱스, 시도 횟수 한도와 격리 상태, PUBLISHED 행 정리 배치.

### 3-9. outbox payload 컬럼이 tinytext로 잡혔던 버그

- **원인**: `@Lob String`에 길이를 주지 않으면 Hibernate 6은 기본 길이 255를 적용하고, MySQL 방언에서 `tinytext`(최대 255바이트)로 만든다. 실제 payload는 FCM 토큰과 landingUrl이 들어가 약 399바이트 → 삽입이 100% `Data too long`으로 실패, 반응 API가 500을 반환했다(`NotificationOutbox:47-54` 주석, 3단계 첫 실행 96.5% 실패).
- **왜 코드 리뷰와 테스트로 안 보였나**: (1) 코드상으로는 `@Lob`이라 "큰 텍스트"로 보인다 — 매핑 결과는 방언과 기본값에 달려 있다. (2) 테스트는 H2(`MODE=MySQL`, `src/test/resources/application-test.yml:5`)라 실제 MySQL 타입 매핑이 드러나지 않는다. (3) 개발 중엔 토큰이 짧거나 없는 데이터로 돌아 payload가 255바이트를 넘지 않았다. 실제 MySQL + 실제 크기의 데이터 + 부하에서만 나타났다.
- **수정**: `columnDefinition = "LONGTEXT"`(`NotificationOutbox:55-57`). `ddl-auto: update`는 기존 컬럼 타입을 바꾸지 않아 `ALTER TABLE`을 따로 적용했다.
- **현재 코드의 한계**: 이 수정도 아직 커밋되지 않았다(0장 1번). 운영 DB에는 ALTER가 별도로 필요하다.
- **개선하려면**: 스키마 마이그레이션 도구(Flyway 등)로 DDL을 코드로 관리, 통합 테스트를 실제 MySQL(Testcontainers)로.

### 3-10. (추가) 브로커가 메시지를 받았는지 모르는 채로 PUBLISHED가 되는 경우

- **코드 경로**: `convertAndSend`(`RabbitNotificationEventDispatcher:18-19`)는 publisher confirm 설정이 없으면 "소켓에 썼다"까지만 보장한다. 브로커가 저장하기 전에 죽거나, 라우팅되지 않는 메시지(`mandatory=false` 기본)는 조용히 사라질 수 있다. 릴레이는 예외가 없으니 PUBLISHED로 바꾼다.
- **결과**: 드물지만 **아웃박스를 거쳤는데도 유실**될 수 있다.
- **개선하려면**: `spring.rabbitmq.publisher-confirm-type=correlated` + `publisher-returns`, 확인을 받은 뒤에만 PUBLISHED.

### 3-11. (추가) 역직렬화할 수 없는 메시지

- JSON 변환 실패는 Spring AMQP가 치명적 오류로 보고 requeue 없이 버린다(기본 에러 핸들러 동작 — 확인 권장). DLQ가 없으니 흔적 없이 사라진다. DLX를 설정해야 사후 분석이 가능하다.

---

# 4부. 설계 선택과 대안

### 4-1. `@Async` 전용 실행자를 두지 않은 이유, 실무라면

- **측정에서 두지 않은 이유**: "기본값 그대로 `@Async`를 붙였을 때"가 가장 흔한 실수이고, 그 결과(무제한 큐 → 힙 적체 → kill 시 유실)를 재현하는 게 목적이었다.
- **실무 설정**: 전용 `ThreadPoolTaskExecutor` — 코어/최대 스레드, **유한 큐**, 거부 정책(`CallerRunsPolicy`로 자연스러운 역압, 또는 거부 후 아웃박스로 폴백), `setWaitForTasksToCompleteOnShutdown(true)` + 대기 시간(정상 종료 시 큐 비우기), 커밋 이후 제출(`@TransactionalEventListener(AFTER_COMMIT)` + `@Async`).
- **그래도 남는 한계**: 어떤 설정도 `kill -9`와 OOM에는 힙의 작업을 살리지 못한다. **내구성이 필요하면 프로세스 밖에 적어야 한다** → 아웃박스로 간 이유.

### 4-2. 아웃박스 폴링 vs CDC(Debezium)

| | 폴링(현재) | CDC |
| --- | --- | --- |
| 방식 | 1초마다 PENDING 조회 | DB binlog를 읽어 변경을 스트리밍 |
| 장점 | 추가 인프라 없음, 이해·디버깅 쉬움 | 폴링 부하·지연 없음, 순서 보장 쉬움 |
| 비용 | 폴링 쿼리 부하, 최대 1초 지연, 상태 갱신 필요 | Kafka Connect 등 인프라, binlog 설정, 운영 난도 |
- **선택 이유**: 단일 서버, 초당 수십 건 규모에서 CDC 인프라는 과하다. 1초 지연은 알림에 문제가 없다.

### 4-3. 릴레이 멀티스레드화 vs 외부 I/O를 컨슈머로 이동

- **릴레이를 멀티스레드로**: 스레드 풀·실패 처리·행 분배(`SKIP LOCKED`)를 직접 구현해야 한다. 외부 호출이 여전히 DB 트랜잭션·행 락 안에 남는다(2-3의 S5 문제). 중복 구조는 그대로.
- **컨슈머로 이동(선택)**: 릴레이는 빠른 브로커 발행만 한다. 병렬화는 RabbitMQ 컨슈머 동시성 설정 하나로 해결. 외부 호출이 DB 트랜잭션 밖으로 나간다. 브로커가 버퍼가 되어 FCM이 느려져도 DB에 부담이 없다.
- **비용**: 브로커라는 운영 대상이 하나 늘고, 브로커 자체의 at-least-once 때문에 **받는 쪽 멱등성이 필수**가 된다.
- **포트폴리오 비고와의 관계**: 비고는 "병렬화만으로는 중복이 해결되지 않는다"는 이유로 릴레이 멀티스레드화를 기각했다. 실제 해결도 "병렬화(컨슈머) + 중복 방지(멱등성)"를 **따로** 둔 것이다 — 모순이 아니라 같은 결론이다.

### 4-4. RabbitMQ vs Kafka vs Redis Streams vs SQS

| | 강점 | 이 프로젝트에서의 판단 |
| --- | --- | --- |
| **RabbitMQ(선택)** | 메시지 단위 ack, 라우팅, 소비자 수 조절 쉬움 | 이미 스택에 있었다(`RabbitMqConfig` 이력 2026-03-27부터). 건별 신뢰 전달이 목적 |
| Kafka | 대용량 스트림, 재생(replay), 파티션 순서 | 단일 서버에 브로커·코디네이터 운영 부담. 초당 수십 건에 과함 |
| Redis Streams | 이미 쓰는 Redis, consumer group, pending list | 가능한 대안. 다만 Redis 영속성 설정에 전달 보장이 묶이고, 멱등 키와 같은 저장소라 Redis 장애가 두 기능을 동시에 끊는다 |
| SQS | 완전 관리형, DLQ 내장 | AWS 종속·비용, 로컬 재현 어려움. 당시 인프라가 단일 EC2 도커 구성 |
- **면접 답변 팁**: "Kafka 대신"만 말하지 말고, "이미 운영 중이던 RabbitMQ로 충분했고, 필요한 건 대용량이 아니라 건별 ack와 소비자 확장이었다"고 말할 것.

### 4-5. Redis SETNX vs DB 유니크 제약 vs 발송 이력 테이블

| | 장점 | 단점 |
| --- | --- | --- |
| **Redis SETNX + TTL(선택)** | 원자적 한 번의 왕복, 자동 정리, 컨슈머 20개에도 빠름 | 영속성이 약함(재시작 시 키 유실 → 중복 가능), 장애 시 발송 중단 |
| DB 유니크(처리 이력 insert) | 내구성, 감사 추적 | 컨슈머마다 DB 쓰기·커넥션 사용, 정리 배치 필요 |
| 아웃박스 행 자체에 발송 상태 기록 | 새 저장소 없음 | 컨슈머가 MySQL에 의존, 락 경합 |
- **선택 이유**: 알림 중복의 비용(사용자 불편)은 결제 중복만큼 치명적이지 않다. 속도와 단순함이 이 정도 약한 내구성보다 중요하다고 판단.

### 4-6. claim을 발송 전에 할지, 발송 후에 기록할지

| | 발송 전 선점(현재) | 발송 후 기록 |
| --- | --- | --- |
| 선점 후 발송 전 죽음 | **유실**(키가 남아 재배달을 막음) | — |
| 발송 후 기록 전 죽음 | 중복 없음 | **중복**(키가 없어 재배달이 다시 보냄) |
| 동시 두 건 도착 | 하나만 보냄 | 둘 다 보낼 수 있음(확인과 기록 사이 틈) |
- **현재 코드는 중복 방지 쪽으로 기울었고, 그 대가로 좁은 유실 구간을 받아들였다.** 이 트레이드오프를 스스로 설명할 수 있어야 한다. 개선안은 3-2의 PROCESSING/DONE 2단계 상태.

---

# 5부. 예상 면접 질문

답변은 말하듯이 적었다. 근거는 괄호 안 `파일:줄`.

## A. 기초

**Q1. 동기 호출이 왜 응답 시간을 그렇게까지 늘렸나요?**
> 요청 스레드가 FCM 응답을 기다리는 동안 DB 트랜잭션도 열려 있어서 커넥션을 쥐고 있었습니다. 커넥션 풀이 10개라 요청 하나가 300ms씩 쥐면 초당 33건이 한계인데, 250건을 넣으니 나머지는 커넥션을 기다리며 쌓였습니다. 실측 처리량 32.7 rps가 그 계산과 맞았습니다. (`ReactionService:132-148`, `SyncPushNotificationSender:64-72`)
- 꼬리: *톰캣 스레드를 늘리면요?* → 기다리는 스레드만 늘어납니다. 병목이 커넥션이라 191개가 이미 커넥션을 기다리고 있었습니다.
- 꼬리: *커넥션 풀을 늘리면요?* → 일시적으로 나아지지만 DB 최대 연결 수와 메모리가 한계고, 근본은 "트랜잭션 안에서 외부 호출을 기다리는 구조"라 발송을 밖으로 빼는 게 맞다고 봤습니다.

**Q2. @Async는 어떻게 동작하고 왜 같은 클래스에서 부르면 안 되나요?**
> 스프링이 빈을 프록시로 감싸고 프록시가 호출을 가로채 실행자에 넘깁니다. 같은 클래스 안에서 this로 부르면 프록시를 거치지 않아 그냥 동기로 실행됩니다. 그래서 발송 부분을 별도 빈으로 뺐습니다. (`AsyncPushDispatcher:17-20, 33-34`)
- 꼬리: *@Transactional도 같은가요?* → 네, 그래서 아웃박스 발행도 건별 트랜잭션을 위해 `NotificationOutboxItemPublisher`로 분리했습니다. (`:15-18`)

**Q3. 기본 @Async 실행자의 문제가 뭔가요?**
> 코어 스레드 8개에 큐가 무제한입니다. 호출은 항상 즉시 성공하니 처리보다 유입이 빠르면 힙에 계속 쌓이고, 최대 스레드 설정은 큐가 차야 쓰이는데 큐가 안 차니 의미가 없습니다. 프로세스가 죽으면 큐가 그대로 사라집니다. 실제로 부하 끝에 6,890건이 쌓여 있었고 kill 후 47.5%가 유실됐습니다. (`MeasureAsyncConfig:11-16`)
- 꼬리: *실무라면 어떻게 설정하나요?* → 유한 큐와 거부 정책, 정상 종료 시 대기를 설정합니다. 다만 kill -9에는 어떤 설정도 소용없어서 내구성이 필요하면 아웃박스로 갑니다.

**Q4. 아웃박스 패턴을 한 문장으로 설명하면?**
> 외부로 보낼 메시지를 비즈니스 데이터와 같은 트랜잭션으로 DB에 적어두고, 별도 작업이 나중에 꺼내 보내는 패턴입니다. "커밋됐으면 보낼 일이 반드시 남아 있다"를 보장합니다. (`NotificationService:135`, `NotificationOutbox:74-76`)
- 꼬리: *왜 커밋 후 바로 보내면 안 되나요?* → 커밋과 발송 사이에 죽으면 유실되고, 예전에 그렇게 했을 때 발행 예외가 커밋된 요청을 500으로 보이게 하는 문제도 있었습니다. (`NotificationService:100-103`)

**Q5. at-least-once, at-most-once, exactly-once 차이는?**
> 최대 한 번은 유실될 수 있지만 중복은 없고, 최소 한 번은 유실은 없지만 중복될 수 있습니다. 정확히 한 번은 외부 API가 끼는 분산 환경에서는 일반적으로 보장할 수 없어서, 보통 최소 한 번 전달에 받는 쪽 멱등성을 더해 결과적으로 한 번만 효과가 나게 만듭니다.

**Q6. 멱등성이 뭐고 여기선 어떻게 구현했나요?**
> 같은 요청을 여러 번 해도 한 번 한 것과 같은 결과가 나는 성질입니다. 컨슈머가 발송 직전에 알림 ID를 Redis에 SET NX로 선점하고, 이미 있으면 건너뜁니다. 만료는 24시간입니다. (`NotificationConsumer:37-40`, `NotificationDeduplicator:41-49`)
- 꼬리: *SETNX 후 EXPIRE를 따로 하면요?* → 둘 사이에 죽으면 만료 없는 키가 남습니다. 저는 `setIfAbsent(key, value, ttl)`로 한 명령으로 처리했습니다.

**Q7. RabbitMQ의 exchange, queue, binding을 설명해 주세요.**
> 생산자는 exchange에 보내고, exchange가 binding 규칙(routing key)에 따라 큐로 나눕니다. 컨슈머는 큐에서 받습니다. 저는 topic exchange 하나, durable 큐 하나를 routing key로 묶었습니다. (`RabbitMqConfig:17-29`)
- 꼬리: *durable이면 메시지도 안 사라지나요?* → durable은 큐 정의가 살아남는 것이고, 메시지는 persistent로 보내야 디스크에 남습니다. Spring은 기본이 persistent입니다(확인 권장).

**Q8. ack는 언제 가나요?**
> 설정을 따로 안 해서 Spring AMQP 기본 AUTO 모드입니다. 리스너 메서드가 정상 반환하면 ack, 예외를 던지면 reject 후 다시 큐에 넣습니다. (`NotificationConsumer:24-60`, 설정 없음)
- 꼬리: *그럼 예외가 계속 나면?* → 재시도 제한이 없어 무한 재배달됩니다. 재시도 인터셉터와 DLQ를 설정하는 게 다음 과제입니다.

**Q9. p95를 왜 봤나요?**
> 평균은 느린 소수를 가립니다. 사용자가 실제로 겪는 최악에 가까운 지연은 꼬리에서 드러나서 p95를 기준으로 삼았습니다.

**Q10. k6에서 constant-arrival-rate를 쓴 이유는?**
> VU 고정 방식은 서버가 느려지면 요청도 같이 느려져서 과부하가 가려집니다. 도착률을 고정해야 "초당 250건이 들어오는데 서버가 못 받는다"가 드러납니다. 1단계의 12,139건은 k6가 서버에 보내지도 못한 요청 수입니다. (`notification-stage34-outbox-rabbit.js:22-27`)

## B. 코드

**Q11. 반응 요청 하나가 들어오면 4단계에서 무슨 일이 일어나나요?**
> 반응 저장, 카운트 갱신, 알림 문서 저장(MongoDB), 아웃박스 PENDING 행 저장까지가 요청 트랜잭션입니다. 커밋되면 응답합니다. 이후 릴레이가 1초마다 PENDING을 읽어 행을 잠그고 RabbitMQ에 발행한 뒤 PUBLISHED로 바꿉니다. 컨슈머가 메시지를 받아 Redis로 선점하고 FCM을 보내고, 정상 반환하면 ack가 갑니다. (2-4 표)

**Q12. 알림 문서는 MongoDB, 아웃박스는 MySQL인데 정합성은?**
> Mongo는 standalone이라 MySQL 트랜잭션에 묶이지 않습니다. 그래서 Mongo를 먼저 저장하고 실패하면 예외로 MySQL을 롤백시킵니다. 반대로 Mongo 성공 후 MySQL이 롤백되면 고아 문서가 남는데, 그건 허용 범위로 정했습니다. 발행에 필요한 건 전부 아웃박스 payload에 있어서 고아 문서가 발송을 일으키지는 않습니다. (`NotificationService:87-122`)

**Q13. 릴레이가 두 인스턴스에서 동시에 돌면?**
> 후보 id는 잠그지 않고 가져오고, 건별로 SELECT FOR UPDATE로 잠근 뒤 이미 PUBLISHED인지 다시 봅니다. 그래서 정상 발행된 걸 둘 다 보내진 않습니다. 다만 둘이 같은 순서로 집어서 서로 락을 기다리느라 병렬 효과가 없고, 개선하려면 SKIP LOCKED를 쓰겠습니다. (`NotificationOutboxRepository:17-27`, `NotificationOutboxItemPublisher:37-41`)

**Q14. 발행이 실패하면 어떻게 되나요?**
> 예외를 삼키고 최초 실패 시각만 기록한 뒤 커밋합니다. 상태가 PENDING으로 남아 1초 뒤 다시 시도합니다. 롤백에 기대지 않은 이유는 롤백하면 최초 실패 시각까지 사라져서, 처음 실패할 때 한 번만 Discord로 알리는 게 불가능해지기 때문입니다. (`NotificationOutboxItemPublisher:29-56`, `NotificationOutboxRelay:41-47`)
- 꼬리: *계속 실패하는 행은?* → 지금은 무한 재시도입니다. 상태를 두 개만 둔 건 의도였지만(`OutboxStatus:3-8`), 이런 행이 쌓이면 가장 오래된 100개 자리를 차지해 새 알림을 막을 수 있어서 시도 횟수와 격리 상태가 필요합니다.

**Q15. 중복을 어떻게 재현했나요?**
> 발행기에 발송이 끝난 **뒤** 30% 확률로 예외를 던지게 했습니다. 앞에서 던지면 그냥 발송 실패라 재시도로 끝납니다. 이 발행기를 3·4단계가 똑같이 쓰게 해서 조건을 맞췄습니다. (`MeasuringNotificationEventDispatcher:28-29, 51-65`)

**Q16. 1,722건이 재발행됐다는 건 어떻게 셌나요?**
> 주입된 예외가 한 번이라도 난 아웃박스 행은 `failed_at`이 채워지고 PENDING으로 남아 다시 발행됩니다. 그 행 수를 DB에서 직접 셌고, 5,842건 중 29.5%로 주입률 30%와 맞았습니다. 같은 실행에서 Redis의 알림별 발송 카운트가 2 이상인 알림은 0이었습니다. (`NotificationOutbox:66, 93-97`, `MeasurementRecorder:50-61`)

**Q17. 발송 기록을 왜 Redis에 남겼나요?**
> kill 실험에서 죽이는 대상이 앱 프로세스라서, 기록이 앱 메모리에 있으면 증거도 같이 사라집니다. 프로세스 밖에 둬야 재시작 후 "접수한 수 대비 실제로 나간 수"를 셀 수 있습니다. 이건 측정 전용 코드고 운영에는 없습니다. (`MeasurementRecorder:13-19`, `@Profile("measure")`)

**Q18. 측정용 어댑터를 어떻게 끼웠나요?**
> 발송이 `PushSenderPort` 인터페이스로 추상화돼 있어서, measure 프로파일에서만 `@Primary`인 구현을 등록했습니다. 300ms 대기 후 Redis에 기록만 합니다. 발송 코드는 한 줄도 안 바꿨습니다. (`PushSenderPort:8-15`, `MeasuringPushSenderAdapter:26-43`)
- 꼬리: *실제 FCM 지연이 300ms가 아니면?* → 스레드 점유 시간이 이 값에 비례하니 임계점이 바뀝니다. 그래서 결과에 항상 지연값을 같이 적었고, 절대 수치보다 단계 간 비교에 의미를 뒀습니다.

**Q19. 컨슈머에서 FCM 발송이 실패하면요?**
> 솔직히 말씀드리면 지금은 재시도되지 않습니다. 포트 계약상 발송 실패를 예외로 올리지 않고, 컨슈머도 토큰별로 예외를 삼킵니다. 그래서 선점을 되돌리는 코드가 있지만 실제로는 거기까지 가지 않고 정상 ack됩니다. 결과적으로 그 알림은 유실됩니다. 개선하려면 포트가 결과를 돌려주게 하고, 재시도 가능한 실패면 선점을 되돌리고 예외로 nack해서 재시도·DLQ로 보내야 합니다. (`PushSenderPort:11-14`, `FcmTokenService:45-53`, `NotificationConsumer:44-59`)

**Q20. ArchUnit S5 규칙은 뭘 막나요?**
> 트랜잭션 메서드 안에서 `PushSenderPort`를 직접 부르는 걸 막습니다. 커밋 전에 보내면 롤백돼도 알림이 나가기 때문입니다. (`ArchitectureTest:166-169`)
- 꼬리: *한 단계 돌려서 부르면요?* → 못 잡습니다. 직접 호출만 봅니다(`:276-293`). 실제로 1단계 측정 경로와 3단계 direct 경로가 트랜잭션 안에서 간접적으로 FCM을 부릅니다. 정적 규칙의 한계라 코드 리뷰와 측정으로 보완했습니다.

## C. 꼬리질문 / 압박

**Q21. exactly-once를 달성했다고 할 수 있나요?**
> 아니요. 정확한 표현은 "최소 한 번 전달에 멱등 소비자를 더해 중복을 막았다"입니다. 좁은 유실 구간도 남아 있습니다. 선점 후 발송 전에 컨슈머가 죽으면 키가 남아 재배달을 막고, publisher confirm이 없어서 브로커가 받지 못한 메시지를 PUBLISHED로 표시할 수 있고, FCM 발송 실패는 재시도되지 않습니다. 측정한 조건, 즉 발행 후 상태 갱신이 실패하는 상황에서는 중복 0, 유실 0을 확인한 것입니다.

**Q22. 유실 0건, 중복 0건이 한 번 측정으로 증명되나요?**
> 증명은 아닙니다. 한 가지 장애(발행 후 상태 갱신 실패 30%)를 넣은 한 번의 실행에서 5,842건이 그렇게 나왔다는 관측입니다. 4단계에서는 컨슈머 강제 종료, 브로커 재시작, Redis 장애는 넣지 않았습니다. 그래서 반복 측정과 장애 종류별 실험이 더 필요하고, 코드로 보면 3부에서 말씀드린 유실 구간이 이론적으로 존재합니다.
- 꼬리: *그럼 왜 0이라고 썼나요?* → 측정 조건에서 관측된 값이라 그대로 적었고, 조건을 같이 밝히는 게 맞다고 생각합니다.

**Q23. 로컬 단일 인스턴스 측정 결과를 운영에 그대로 적용할 수 있나요?**
> 절대 수치는 아닙니다. FCM 대신 300ms 고정 지연을 썼고, 로컬 한 대에 DB·Redis·RabbitMQ를 같이 띄웠고, 단계마다 한 번씩만 돌렸습니다. 의미가 있는 건 같은 조건에서 단계를 바꿨을 때의 상대 비교, 그리고 유실·중복이 "생기는지 안 생기는지"라는 구조적 결과입니다. 운영 적용 전에는 별도 환경에서 실제 FCM 지연 분포로 다시 재야 합니다.

**Q24. 왜 컨슈머 동시성을 20으로 정했나요?**
> 측정으로 최적값을 찾은 건 아닙니다. 3단계 병목이 발송 스레드 하나였고, 컨슈머 하나가 초당 약 3건을 보내니 20개면 이론상 초당 66건으로 목표 부하를 감당할 수 있겠다고 보고 정했습니다. 실측은 48건이었습니다. 제대로 하려면 5, 10, 20, 40으로 바꿔가며 처리량과 FCM 할당량, 메모리를 보고 정해야 합니다. 그리고 이 값은 현재 측정 프로파일에만 들어가 있습니다.
- 꼬리: *너무 높이면?* → FCM 레이트 리밋에 걸리고, prefetch가 기본 250이라 한 컨슈머가 메시지를 많이 들고 있다가 죽으면 재배달 폭이 커집니다.

**Q25. p95가 RabbitMQ 단계에서 오히려 늘었는데 왜 개선이라고 하나요?**
> 3단계와 4단계는 요청 경로 코드가 완전히 같습니다. 바뀐 건 요청 뒤의 발송 경로뿐이라 응답 시간 차이(11.96초 → 13.41초)는 한 번씩 돌린 측정의 편차로 봤습니다. 4단계의 개선 대상은 응답 시간이 아니라 발송 처리량(3.1 → 48건/s)과 중복 0입니다. 솔직히 응답 시간 자체는 3·4단계 모두 여전히 나쁩니다. 톰캣 busy 200, 커넥션 대기 191이 네 단계 내내 같았고, 요청 경로의 병목은 커넥션 풀이라 알림 파이프라인과 별개로 풀어야 할 문제입니다.

**Q26. 처음부터 동기 → @Async → 아웃박스 → RabbitMQ 순서로 개발했나요?**
> 아닙니다. 실제 운영 이력은 커밋 후 이벤트로 RabbitMQ를 직접 부르다가 아웃박스로 바꾼 것이고, 동기와 @Async 단계는 "각 방식이 실제로 어떻게 깨지는지"를 보여주려고 측정 프로파일에서 재현한 겁니다. 문제를 추측이 아니라 재현해서 보이는 게 목적이었습니다. (`NotificationService:100-103`, `MeasurePushRouter`)

**Q27. Redis가 죽으면 알림이 어떻게 되나요?**
> 선점 단계에서 예외가 나서 발송하지 않고, 메시지는 다시 큐로 돌아갑니다. 중복도 유실도 없지만, 재시도 제한이 없어 Redis가 돌아올 때까지 재배달이 반복됩니다. 또 Redis가 영속성 없이 재시작되면 키가 사라져 그 뒤 재배달은 중복될 수 있습니다. (`NotificationConsumer:37`, `NotificationDeduplicator:46`)

**Q28. TTL을 24시간으로 잡은 근거는?**
> 브로커 장애로 재배달이 한참 뒤에 올 수 있어서 넉넉히 잡았고, 알림 하나당 키 하나라 하루치를 들고 있어도 메모리 부담이 작다고 봤습니다. 정량적으로 정한 값은 아닙니다. 그리고 24시간은 "선점 후 죽으면 24시간 동안 막힌다"는 뜻이기도 해서, 처리 중 상태는 짧게, 완료 상태만 길게 가져가는 게 더 낫습니다. (`NotificationDeduplicator:30-32`)

**Q29. 아웃박스 테이블이 계속 커지지 않나요?**
> 커집니다. 추적성을 위해 PUBLISHED 행을 지우지 않았고 정리 배치는 필요해지면 넣기로 했습니다. 그런데 status 인덱스도 없어서 행이 쌓이면 PENDING 조회가 느려집니다. (status, id) 인덱스와 보관 기간을 둔 정리 작업이 필요합니다. (`NotificationOutbox:24-27`, `NotificationOutboxRepository:19-20`)

**Q30. 왜 Kafka가 아니라 RabbitMQ인가요?**
> 필요한 건 대용량 스트림이 아니라 알림 한 건 한 건의 ack와 소비자 수 조절이었습니다. RabbitMQ는 이미 스택에 있었고, 단일 서버에서 Kafka를 따로 운영하는 부담이 컸습니다. Redis Streams도 후보였지만 멱등 키와 같은 저장소라 Redis 장애가 전달과 중복 방지를 동시에 끊는 점이 걸렸습니다.

**Q31. 발행을 트랜잭션 안에서 하는데, 그건 S5가 막으려던 문제 아닌가요?**
> 운영 발행은 FCM이 아니라 브로커로 보내는 거라 짧고, 행 락을 잡은 상태에서 보내야 "보낸 뒤 PUBLISHED"를 같은 트랜잭션에서 표시할 수 있습니다. 대신 브로커가 느려지면 커넥션과 락을 오래 쥡니다. 측정용 direct 경로는 실제 외부 호출을 트랜잭션 안에서 해서 3단계 처리량이 낮았던 원인이기도 합니다. (`NotificationOutboxItemPublisher:35-49`)

**Q32. 이 사례에서 가장 아쉬운 점은?**
> 발송 실패를 구분하지 못하는 포트 설계입니다. 실패를 삼키게 만들어서 선점 되돌리기와 재시도가 실제로 동작하지 않습니다. 그리고 4단계 장애 실험이 한 종류뿐이었습니다. 컨슈머 강제 종료와 브로커 재시작까지 넣어서 "유실 0"을 검증했어야 합니다.

**Q33. tinytext 버그는 어떻게 발견했고 왜 테스트로 못 잡았나요?**
> 3단계 첫 실행에서 96.5%가 500으로 실패해서 로그를 보고 찾았습니다. `@Lob String`에 길이가 없으면 Hibernate 6이 255로 잡아 MySQL에서 tinytext가 되는데, 실제 payload는 약 399바이트였습니다. 테스트는 H2라 이 매핑이 드러나지 않았고, 개발 데이터는 payload가 짧았습니다. `columnDefinition = "LONGTEXT"`로 고치고 기존 테이블은 ALTER로 바꿨습니다. (`NotificationOutbox:47-57`)

---

# 6부. 포트폴리오 문장 점검

| # | 포트폴리오 문장 | 판단 | 근거 | 수정안 |
| --- | --- | --- | --- | --- |
| 1 | "목표 15,000건 중 **12,139건은 아예 보내지지도 못했습니다**" | 오해 소지 | 12,139는 k6 `dropped_iterations` — **요청**이 서버에 도달하지 못한 수. 알림 발송 실패가 아님 | "초당 250건 중 서버가 받아내지 못해 **요청 12,139건은 전송조차 되지 못했습니다**" |
| 2 | "메모리(힙)에만 쌓여 있던 발송 대기열(**최대** 6,890건)" | 경미 | 문서는 "부하가 끝난 시점에 6,890건" | "부하 종료 시점에 6,890건이 쌓여 있던" |
| 3 | 해결 1 "FCM 발송 루프만 @Async로 떼어 요청 스레드에서 제거했습니다" | 불일치(맥락) | 1·2단계는 운영에 적용된 적 없는 측정용 재현. 제출은 커밋 전에 일어남 | 해결 1·2 앞에 "각 방식이 어떻게 깨지는지 측정 환경에서 재현했다"는 전제를 한 줄 두기 |
| 4 | 원인 1 "그 스레드가 쥐고 있던 DB 커넥션도 반납되지 못해" | 정확 | FCM 호출이 `testReactToRecord` 트랜잭션 안(`ReactionService:132-148`) | 유지. "트랜잭션 안에서 FCM을 기다려"를 넣으면 더 명확 |
| 5 | 해결 3 "컨슈머 동시성은 1에서 20으로 올려" | **불일치** | `application-measure.yml`에만 있음. 운영 설정 없음 → 운영은 1 | 운영 설정에 반영하거나 "측정 환경에서 컨슈머 20개로" |
| 6 | 해결 3 멱등성 처리 전체 | **불일치(배포 상태)** | `NotificationDeduplicator` untracked, 컨슈머 수정 미커밋 | 커밋·배포 후 인용. 전까지는 "구현·측정 완료, 반영 전" |
| 7 | 평가 "최종 단계에서 유실 0건, 중복 0건" | 과장 위험 | 4단계는 상태 갱신 실패 주입만, 강제 종료 실험 없음. 코드상 유실 구간 존재(3-2, 3-4, 3-10) | "상태 갱신 실패를 30% 주입한 조건에서 유실·중복 0건" |
| 8 | 평가 "그중 실제로 두 번 발송된 알림은 0건" | 정확 | Redis 알림별 카운트 기준 | 유지 |
| 9 | 표 "RabbitMQ+멱등 p95 13.41s" | 공격받기 쉬움 | 3단계보다 늘어남 | 표 아래 한 줄: "3·4단계의 요청 경로는 동일하며 p95 차이는 단일 측정 편차 범위. 4단계의 개선 지표는 발송 처리량과 중복" |
| 10 | "발송 처리량 약 15.5배(초당 3.1건 → 48건)" | 정확 | 3.1 → ~48 | 유지(문서의 "16배"와 표기 통일) |
| 11 | 비고 "Kafka 운영 부담" | 약함 | 실제로는 RabbitMQ가 이미 스택에 있었음(2026-03-27 이력) | "이미 운영 중이던 RabbitMQ로 건별 ack·소비자 확장이 충분했다" 추가 |
| 12 | 해결 3 "이미 등록된 알림이면 보내지 않고 건너뜁니다" | 정확하나 불완전 | 발송 실패 시 되돌림이 실제로 동작하지 않음 | 면접 대비용으로만 기억(Q19). 본문 수정은 선택 |
| 13 | 측정 "반복 4단계 × 각 1회" | 정직함 | — | 유지. 면접에서 먼저 한계로 언급하면 신뢰를 얻는다 |
| 14 | 원인 3 "외부 발송과 DB 상태 갱신이 한 번에 묶여 처리되지 않아" | 정확 | `NotificationOutboxItemPublisher:44-56` | 유지 |

---

## 외워둘 숫자

| 숫자 | 의미 | 측정 조건 / 출처 |
| --- | --- | --- |
| 250 req/s, 60초 | 부하 | k6 constant-arrival-rate, VU 200~1000, 전 단계 동일 |
| 300ms | FCM 대체 지연 | `measure.push.latency-ms`, 전 단계 동일 |
| 200 / 10 | 톰캣 스레드 / HikariCP 커넥션 | Spring Boot 기본값, 로컬 단일 인스턴스 |
| **33.83s** | 1단계 p95 | `stage1-sync-summary.json` |
| 32.7 rps | 1단계 처리량 | ≈ 커넥션 10 ÷ 0.3s의 이론 한계 |
| **12,139** | 1단계에서 k6가 보내지 못한 요청 | `dropped_iterations` |
| 200 / **191** | 1단계 톰캣 busy / 커넥션 대기 | Grafana. 4단계까지 동일 |
| **9.56s** | 2단계 p95 | `notification-pipeline.md` 표만 남음(원본 JSON 덮어써짐) |
| 26건/s | 2단계 발송 처리량 | 8스레드 × (1/0.3s) ≈ 26.7 |
| 6,890 | 2단계 부하 종료 시점 힙 큐 | `executor_queued_tasks` |
| **8,552 / 4,064 / 47.5%** | 2단계 접수 / 유실 / 유실률 | kill -9 후 재시작, Redis 카운터 |
| 11.96s | 3단계 p95 | `stage3-outbox-summary.json` |
| **3.1건/s** | 3단계 발송 처리량 | 스케줄러 스레드 1개 × 300ms 직렬 |
| 6,646 | 3단계 kill 후 DB에 살아남은 PENDING | 유실 0의 근거 |
| **28** | 3단계 중복 발송 | 발행 후 상태 갱신 실패 30% 주입 |
| 96.5% | 3단계 첫 실행 실패율(tinytext 버그) | payload 약 399바이트 vs 255 |
| 13.41s | 4단계 p95 | `stage4-rabbit-summary.json`, 요청 경로는 3단계와 동일 |
| **약 48건/s** | 4단계 발송 처리량 | 컨슈머 20(이론 ~66) |
| **15.5배** | 3단계 대비 발송 처리량 | 3.1 → 48 |
| **5,842** | 4단계 발송 확정 = 고유 발송 수 | 유실 0 |
| **1,722 (29.5%)** | 4단계 재발행된 아웃박스 행 | `failed_at` 채워진 행, 주입률 30%와 일치 |
| **0** | 4단계 중복 발송 | Redis 알림별 발송 카운트 ≥2인 알림 수 |
| 24시간 | 멱등 키 TTL | `NotificationDeduplicator:32` |
| 1초 / 100건 | 릴레이 주기 / 배치 | `NotificationOutboxRelay:29, 35` |


4단계 핵심 코드가 아직 커밋되지 않았습니다. NotificationDeduplicator와 발행기 분리 코드는 git이 추적하지 않는 상태(untracked)이고, 컨슈머 수정과 LONGTEXT 수정도 커밋 전입니다. 면접관이 "코드로 보여달라"고 하면 지금은 보여드릴 수 없습니다.
컨슈머 동시성 20은 측정 프로파일에만 있습니다. 운영 설정에는 이 값이 없어서 운영은 기본값 1로 돕니다. 포트폴리오에는 "20으로 올렸다"고만 쓰여 있습니다.
발송이 실패해도 선점을 되돌리는 코드는 실제로 실행되지 않습니다. 발송 포트가 실패를 예외로 올리지 않도록 설계돼 있어서, 실패한 알림은 재시도 없이 사라집니다. 게다가 선점을 먼저 하는 구조라, 선점 직후 컨슈머가 죽으면 그 알림은 24시간 동안 다시 보내지지 않습니다. 그래서 "exactly-once를 달성했나"라는 질문에는 "아니다"가 정직한 답이고, 예상 질문 Q21에 그 답변을 적어 뒀습니다.
RabbitMQ의 ack, 재시도, DLQ, 발행 확인(publisher confirm) 설정이 하나도 없습니다. 전부 기본값으로 돕니다. 예외가 나면 같은 메시지가 무한히 재배달될 수 있습니다.