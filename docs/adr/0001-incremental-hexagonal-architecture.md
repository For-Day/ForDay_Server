# ADR-0001. 점진적 헥사고날 전환 (클린 아키텍처 부분 도입)

| 항목 | 내용 |
| --- | --- |
| 상태 | 제안 (Proposed) — 팀 합의 대기 |
| 작성일 | 2026-08-30 |
| 대상 | `ForDay_Server` (Spring Boot 3.5 / Java 17, 353 java files, 11 도메인) |
| 결정 기한 | 스프린트 시작 전 |

---

## 1. 맥락

현재 구조는 도메인별 계층형 패키지다.

```
com.example.ForDay
├── domain/<name>/{controller, dto, entity, repository, service, type, utils, validator}
├── global/{common, config, filter, oauth, firebase, rabbitmq, ai, util}
└── infra/{s3, lambda}
```

바운디드 컨텍스트 분리 자체는 잘 되어 있다. 문제는 **계층 간 의존 방향이 통제되지 않는다**는 점이며, 아래 네 가지로 나타난다.

### 1.1 엔티티가 웹 계층 DTO를 역참조한다

의존성 규칙(안쪽은 바깥쪽을 모른다)의 정면 위반이다. 현재 5개 엔티티가 요청 DTO를 직접 import 한다.

| 엔티티 | 위반 지점 |
| --- | --- |
| `domain/hobby/entity/Hobby.java` | `updateHobby(UpdateHobbyReqDto, Integer)` :125<br>`createNewHobby(User, HobbyCreateReqDto, Integer)` :159<br>`createNewHobbyV2(User, HobbyCreateReqDtoV2.HobbyInfo, int)` :172 |
| `domain/record/entity/ActivityRecord.java` | `updateRecord(Activity, UpdateActivityRecordReqDto)` :88<br>`updateRecordV2(Activity, UpdateActivityRecordReqDtoV2)` :96<br>`of(...)` :112 / `ofV2(...)` :124 |
| `domain/record/entity/RecordImage.java` | `of(ActivityRecord, ActivityRecordReqDtoV2.ActivityImageReqDto, boolean)` :44 |
| `domain/term/entity/UserTermsConsent.java` | `create(RegisterTermsConsentReqDto, String)` :42 |
| `domain/activity/entity/Activity.java` | DTO import |

특히 `ActivityRecord`(record 컨텍스트)가 `hobby.dto.request.RecordActivityReqDto`를 참조해, **다른 컨텍스트의 웹 DTO**에까지 묶여 있다.

**영향** — API 스펙이 바뀌면 도메인 엔티티가 흔들린다. V2/V3 DTO가 생길 때마다 엔티티에 팩토리 오버로드가 누적되고(`createNewHobby` / `createNewHobbyV2`), 엔티티 단위 테스트에 웹 DTO를 조립해야 한다.

### 1.2 도메인 서비스가 인프라를 직접 참조한다 (포트 부재)

S3 · Lambda · FCM · FastAPI(AI) · RestTemplate을 직접 잡는 도메인 파일이 20개 이상이다.

| 외부 자원 | 참조하는 도메인 |
| --- | --- |
| `infra.s3.util.S3Util` | activity, app, friend, reaction, record(v1·v2·v3), user — 8곳 |
| `infra.s3.service.S3Service` | app, hobby(`HobbyCardService`, `HobbyService`) |
| `infra.lambda.invoker.CoverLambdaInvoker` | hobby/service/v1/HobbyService |
| `global.firebase.*` | auth, term, notification, app |
| `global.ai.service.*` | activity, hobby |
| `RestTemplate` (직접 `new`) | `auth/service/AppleService.java:116` |

**영향** — 인터페이스가 없어 도메인 로직을 순수 단위 테스트로 검증할 수 없다. `@SpringBootTest`가 실제 로컬 Redis를 요구하는 현 상황도 같은 뿌리다. `AppleService`는 `new RestTemplate()`을 메서드 안에서 생성해 스텁 주입 지점 자체가 없다.

### 1.3 V1/V2/V3 병렬 구조에서 비즈니스 규칙이 복제된다

버전이 갈릴 때 **서비스 클래스를 통째로 복제**하는 것이 현재 관행이다 (`ActivityRecordService` / `V2` / `V3`, `HobbyService` / `HobbyServiceV2`). record 67개 · hobby 65개 파일 중 상당수가 여기서 나온다.

**영향** — 규칙 하나를 고치면 최대 3곳을 고쳐야 하고, 누락되면 버전별로 동작이 갈린다. 이것이 현재 구조가 만드는 **가장 큰 실질 비용**이다.

### 1.4 비즈니스 판단이 `global/ai`로 새어 나갔다

`global/ai/service/AiUserSummaryService.determine(User, Hobby)`, `AiActivityRecommendService.requestActivityRecommendAI(User, Hobby)` 처럼 횡단 관심사 패키지가 도메인 엔티티를 받아 판단까지 수행한다. 어댑터여야 할 자리에 유스케이스가 섞여 있다.

---

## 2. 결정

**전면 클린 아키텍처 전환은 하지 않는다. 헥사고날(포트-어댑터)의 핵심 두 가지 — 의존성 방향 고정, 외부 연동 포트화 — 만 점진적으로 도입한다.**

구체적으로:

1. **의존 방향을 ArchUnit으로 강제**한다. 문서가 아니라 테스트가 규칙을 지킨다.
2. **엔티티에서 DTO 의존을 제거**한다. 엔티티 팩토리는 원시 타입/도메인 값만 받고, DTO → 파라미터 변환은 애플리케이션 계층 매퍼가 맡는다.
3. **외부 연동에 포트 인터페이스를 둔다.** 인터페이스는 도메인 쪽에, 구현체는 `infra`에 배치한다.
4. **명령(Command)과 조회(Query)를 비대칭으로 다룬다.**
   - 명령: 유스케이스를 경유한다.
   - 조회: **현행 유지.** QueryDSL `Projections` → 응답 DTO 직행을 그대로 둔다.

4번이 이 ADR의 핵심 타협점이다. 조회까지 도메인 모델을 경유시키면 리포지토리 전반에 깔린 조회 최적화가 무너진다.

---

## 3. 고려한 대안

### 대안 A. 현행 유지

- **장점**: 비용 0. 기능 개발 속도에 영향 없음.
- **기각 사유**: 1.3의 버전 중복 비용이 버전이 늘수록 선형 이상으로 증가한다. V4가 생기면 규칙 수정 지점이 4곳이 된다.

### 대안 B. 전면 클린 아키텍처 (도메인 모델 / JPA 엔티티 완전 분리)

`entities` / `usecases` / `interface-adapters` / `frameworks` 4계층을 원칙대로 적용하고, POJO 도메인 모델과 JPA 엔티티를 분리한 뒤 매퍼로 잇는 방식.

- **기각 사유** (이 프로젝트에 한정된 비용):
  - QueryDSL `Projections`로 조회 DTO에 직접 매핑하는 패턴이 리포지토리 전반에 깔려 있다. 도메인 모델을 강제 경유시키면 조회 성능 최적화가 통째로 무너지고, `XxxRepositoryCustom` / `Impl` 3종 구조를 전부 재작성해야 한다.
  - `@SQLRestriction("deleted = false")` 소프트 삭제, `BaseTimeEntity`의 `Asia/Seoul` 타임스탬프, `@DynamicUpdate` 등 **Hibernate 기능에 의존한 도메인 규칙**이 엔티티에 박혀 있다. POJO로 옮기면 이 규칙들을 애플리케이션 코드로 재구현해야 한다.
  - `ddl-auto: update`로 스키마가 엔티티를 따라가고 별도 마이그레이션 도구가 없어, 엔티티 대수술은 곧 스키마 리스크다.
  - 353개 파일을 운영 배포(main → 블루-그린)와 병행해 전환하는 리스크.
- **판단**: 얻는 것(테스트 용이성, 프레임워크 독립성) 대비 비용이 과도하다. 프레임워크 교체 계획도 없다.

### 대안 C. 점진적 헥사고날 — **채택**

1.1 · 1.2 · 1.4를 먼저 해소하고, 1.3은 한 도메인 파일럿으로 효과를 측정한 뒤 확산 여부를 결정한다. 각 단계가 독립적으로 배포 가능하고 언제든 멈출 수 있다.

---

## 4. 실행 계획

### Phase 0 — 규칙을 테스트로 고정 (0.5일)

ArchUnit 의존성 테스트를 먼저 추가한다. 규칙을 나중에 넣으면 그사이 새 위반이 쌓인다.

전체 규칙 목록과 실행 코드는 **[`docs/architecture-rules.md`](../architecture-rules.md)** 에 있다. 이 ADR은 그 규칙을 도입한다는 결정만 기록한다.

- `build.gradle`에 `archunit-junit5` 추가 (현재 미포함)
- **freeze는 도입하지 않는다.** 위반이 25개 파일 수준이고 Phase 1·2에서 전부 제거할 대상이라, violation store 파일을 관리할 이유가 없다.
- 위반이 남아 있는 규칙은 `@ArchIgnore("Phase 1 완료 후 활성화")`로 두고 해당 Phase 종료 시 제거한다.
  > JUnit의 `@Disabled`는 `@ArchTest` 필드에 적용되지 않는다. ArchUnit 자체 애노테이션인 `@ArchIgnore`를 써야 한다.

### Phase 1 — 엔티티에서 DTO 의존 제거 (1~2일)

엔티티 팩토리/수정 메서드의 시그니처를 값 파라미터로 바꾸고, 변환은 서비스 계층 매퍼로 옮긴다.

```java
// AS-IS — domain/hobby/entity/Hobby.java:172
public static Hobby createNewHobbyV2(User user, HobbyCreateReqDtoV2.HobbyInfo info, int sequence)

// TO-BE
public static Hobby createNewHobbyV2(User user, Long hobbyInfoId, String hobbyName, int sequence)
// 호출부(HobbyServiceV2)에서 DTO를 풀어 넘기거나, 파라미터가 많으면 도메인 커맨드 객체를 둔다
```

대상은 1.1 표의 5개 엔티티다. 필드 수가 많은 `updateHobby` / `updateRecordV2`는 커맨드 record(예: `HobbyUpdateCommand`)를 둔다.

**커맨드 객체 위치: `domain/<name>/command`** — 기존 `domain/<name>/{dto, type, utils, validator}` 와 같은 레벨이다. Phase 3을 축소 착수하기로 해 `application/`을 전 도메인에 두지 않으므로, 기존 패키지 컨벤션을 따르는 편이 일관적이다.

함께 처리할 것: `User.java:147`이 `org.springframework.util.StringUtils.hasText`를 쓴다. `nickname != null && !nickname.isBlank()`로 바꿔 엔티티의 Spring 의존을 없앤다 (규칙 D3). `RefreshToken`의 `@RedisHash`는 Redis 해시 엔티티에 필수이므로 규칙 예외로 남긴다.

**동작 변경 없는 순수 리팩토링**이므로 기존 테스트가 그대로 통과해야 한다.

### Phase 2 — 외부 연동 포트화 (2~3일)

**포트 위치 규칙 — 2개 이상 도메인이 쓰면 `global/port`, 단일 도메인 전용이면 `domain/<name>/port`.** 사용 도메인 수를 세어 정했다. `ImageStoragePort`를 8개 도메인에 중복 정의하는 것은 무의미하고, hobby 전용 Lambda 포트를 공용 자리에 두면 응집도가 떨어진다.

| 포트 | 위치 | 어댑터 | 사용 도메인 수 | 대체하는 현재 의존 |
| --- | --- | --- | --- | --- |
| `ImageStoragePort` | `global/port` | `S3ImageStorageAdapter` | 8 | `S3Service`, `S3Util`의 I/O 부분 |
| `PushSenderPort` | `global/port` | `FcmPushSenderAdapter` | 4 | `global.firebase.*` |
| `AiInsightPort` | `global/port` | `FastApiAiInsightAdapter` | 2 | `global.ai.service.*` |
| `CoverGeneratorPort` | `domain/hobby/port` | `LambdaCoverGeneratorAdapter` | 1 | `CoverLambdaInvoker` |
| `AppleIdentityPort` | `domain/auth/port` | `AppleRestAdapter` | 1 | `AppleService`의 `new RestTemplate()` |

**주의 — `S3Util`은 통째로 포트화하지 않는다.** 이 클래스는 성격이 다른 셋이 섞여 있다.

- `toXxxResizedUrl(...)` — 순수 문자열 변환. 외부 I/O 없음 → **포트 불필요**, 도메인 유틸로 남긴다.
- `validateS3Image(...)` — S3 존재 확인(I/O) → `ImageStoragePort.exists(key)`
- `registerS3DeletionAfterCommit(...)` — 트랜잭션 동기화 + 삭제 → 어댑터로 이동

8개 도메인이 `S3Util`을 참조하지만 대부분 URL 변환만 쓰므로, 실제 수정 범위는 그보다 작다. 착수 전 사용처를 위 세 분류로 나눌 것.

`AiUserSummaryService.determine(User, Hobby)`의 판단 로직은 hobby 도메인으로 옮기고, `global/ai`에는 호출/직렬화만 남긴다 (1.4 해소).

### Phase 3 — `record` V2/V3 공유 유스케이스 추출 (2~3일)

**중복 비율 측정 결과 (2026-08-30)**

`ActivityRecordServiceV3`는 전체 98줄 / public 메서드 1개다. V2와 비교하면:

- private 헬퍼 3개(`validateCondition`, `validateAccess`, `isStoryContext`)는 V2와 **바이트 단위로 동일**하다. 유일한 차이는 `if(` vs `if (` 공백 하나.
- `getRecordDetail` 본문도 검증·조회 로직이 전부 같고, 실제 차이는 셋뿐이다.
  1. 스크랩 판정을 `isScraped(detail, user)` 대신 `existsByScrap(...)` 직접 호출
  2. `recordImageRepository.findAllByActivityRecordIdOrderByImageOrderAsc` 이미지 목록 추가 조회
  3. 응답 DTO 조립 (`GetRecordDetailResDtoV2.of` → `V3.of`)

→ **비즈니스 규칙은 100% 동일하고 데이터 수집과 DTO 조립만 다르다.** 추출 가치가 명확하다.

반면 **V1 ↔ V2는 통합하지 않는다.** `deleteActivityRecord`를 비교하면 골격(소유 검증 → 중복 삭제 체크 → 연관 3종 삭제 → 오늘 여부 분기 hard/soft delete → S3 삭제 예약 → 캐시 evict 2종)은 같지만, 단일 이미지(`record.getImageUrl()`) → 다중 이미지(`RecordImage` 목록)라는 **데이터 모델 변경**이 끼어 있다. 억지로 합치면 파라미터가 오염된다. V1은 구버전 앱 전용이므로 그대로 둔다.

**결정 — 범위를 V2/V3로 좁혀 착수한다.**

```
domain/record/
├── application/
│   ├── RecordAccessPolicy      # validateCondition / validateAccess / isStoryContext
│   └── GetRecordDetailUseCase  # 검증 + 조회 (응답 DTO 무관)
├── service/v1/  ← 손대지 않음 (구버전 앱)
├── service/v2/  ← UseCase 호출 + V2 DTO 조립
└── service/v3/  ← UseCase 호출 + V3 DTO 조립
```

`application/`은 이번에 `record` 도메인에만 만든다. 다른 도메인으로의 확산(Phase 4, hobby 등)은 이 결과를 보고 별도 ADR로 판단한다.

---

## 5. 결과

**긍정**

- 의존 방향이 테스트로 강제되어 회귀가 막힌다.
- 도메인 로직을 Mockito 단위 테스트로 검증 가능해진다 (`@SpringBootTest` + 실제 Redis 의존 축소).
- API 버전 추가 시 엔티티를 건드리지 않는다.
- Phase 3까지 가면 규칙의 단일 출처가 생겨 버전 간 동작 불일치가 사라진다.

**부정 / 감수하는 것**

- 매퍼 클래스와 포트 인터페이스만큼 파일 수가 늘어난다 (Phase 1·2 합계 대략 15~25개 추가 예상).
- 조회 경로는 여전히 엔티티/Projection이 응답 DTO까지 직행하므로 **아키텍처가 대칭이 아니다.** 이는 의도된 타협이며, 신규 참여자에게 이 문서로 설명한다.
- Phase 1·2 진행 중 동일 파일을 건드리는 기능 브랜치와 충돌 가능. `dev` 병합 주기를 짧게 가져간다.

**하지 않기로 한 것 (명시)**

- JPA 엔티티와 도메인 모델 분리
- 리포지토리 조회 경로의 유스케이스 경유
- 멀티모듈 분리
- 기존 V1 API 동작 변경 (구버전 앱이 사용 중)

---

## 6. 검증 방법

- Phase 0의 ArchUnit 규칙이 Phase 2 종료 시점에 전부 활성 상태로 통과한다.
- Phase 1·2는 동작 변경이 없으므로 **기존 테스트 수정 없이** 통과해야 한다. 테스트를 고쳐야 한다면 리팩토링 범위를 넘은 것이다.
- Phase 2 완료 후, 최소 한 개 도메인 서비스가 Spring 컨텍스트 없이 단위 테스트되는 것을 확인한다.

---

## 7. 세부 결정 (2026-08-30 확정)

| # | 항목 | 결정 | 근거 |
| --- | --- | --- | --- |
| 1 | Phase 3 범위 | **V2/V3만 축소 착수** (2~3일). V1 제외 | V2↔V3 헬퍼가 바이트 단위 동일, 규칙 100% 일치. V1↔V2는 단일→다중 이미지 모델 변경이 섞여 통합 시 파라미터 오염 |
| 2 | 커맨드 객체 위치 | **`domain/<name>/command`** | 기존 `dto`/`type`/`utils`/`validator`와 같은 레벨. `application/`을 전 도메인에 두지 않기로 했으므로 |
| 3 | 포트 위치 | **2개 이상 도메인 사용 시 `global/port`, 단일 전용은 `domain/<name>/port`** | S3 8개 · FCM 4개 · AI 2개 도메인이 공유. Lambda·Apple은 각 1개 도메인 전용 |
| 4 | ArchUnit 위반 처리 | **freeze 미도입.** `@ArchIgnore` + 단계별 활성화 | 위반이 25개 파일 수준이고 Phase 1·2에서 전부 제거 예정. violation store 관리 비용이 이득보다 큼 |

## 8. 남은 미결 사항

- [ ] Phase 4(hobby 확산) 여부 — Phase 3 완료 후 별도 ADR
- [ ] `S3Util` 사용처 8곳을 URL 변환 / I/O / 트랜잭션 동기화 삭제로 분류 (Phase 2 착수 전)

## 9. 관련 문서

- [`docs/architecture-rules.md`](../architecture-rules.md) — SOLID 기반 아키텍처 규칙과 ArchUnit 실행 코드
