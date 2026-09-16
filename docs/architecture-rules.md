# 아키텍처 규칙 (SOLID 기반)

이 문서는 `ForDay_Server`가 지키는 아키텍처 규칙과, 그중 **기계로 강제되는 규칙의 실행 코드**를 담는다.
배경과 결정 과정은 [ADR-0001](./adr/0001-incremental-hexagonal-architecture.md) 참고.

> **원칙** — 규칙은 문서가 아니라 테스트가 지킨다. 여기 적혔지만 ArchUnit으로 검증되지 않는 항목은 §7 리뷰 체크리스트로 따로 분리했다. 지켜지지 않을 규칙을 강제되는 규칙인 척 적지 않는다.

---

## 1. 도입 방법

`build.gradle`:

```gradle
testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'  // 버전은 최신 확인
```

테스트 위치: `src/test/java/com/example/ForDay/architecture/ArchitectureTest.java`

실행: `./gradlew test --tests "ArchitectureTest"`

**위반이 남아 있는 규칙은 `@ArchIgnore("Phase N 완료 후 활성화")`로 표시**하고, 해당 Phase가 끝나면 애노테이션을 제거한다.

> JUnit의 `@Disabled`는 `@ArchTest` 필드에 적용되지 않는다. ArchUnit 자체 애노테이션인 `@ArchIgnore`(`com.tngtech.archunit.junit.ArchIgnore`)를 써야 한다.

freeze(violation store)는 쓰지 않는다. 현재 위반이 25개 파일 수준이고 Phase 1·2에서 전부 제거할 대상이라, 저장 파일 관리 비용이 이득보다 크다.

---

## 2. 규칙 목록

| ID | 원칙 | 규칙 | 현재 상태 |
| --- | --- | --- | --- |
| D1 | DIP | `domain`은 `infra` 구현체에 의존하지 않는다 | 🔴 위반 20+ → Phase 2 |
| D2 | DIP | 엔티티는 `dto`에 의존하지 않는다 | 🔴 위반 5 → Phase 1 |
| D3 | DIP | 엔티티는 Spring에 의존하지 않는다 | 🔴 위반 1 → Phase 1 |
| D4 | DIP | 포트(`*Port`)는 인터페이스여야 한다 | 🟢 활성 (신규) |
| D5 | DIP | 도메인은 `RestTemplate`을 직접 쓰지 않는다 | 🔴 위반 1 → Phase 2 |
| S1 | SRP | 클래스당 주입 의존성 ≤ 8 | 🟡 레거시 7개 예외 |
| S2 | SRP | 컨트롤러는 리포지토리에 직접 의존하지 않는다 | 🟢 활성 |
| S3 | SRP | 컨트롤러는 대응 `Docs` 인터페이스를 구현한다 | 🟢 활성 |
| S4 | SRP | 도메인 간 순환 의존이 없다 | 🔴 미측정 → Phase 3 이후 |
| S5 | SRP | `@Transactional` 메서드는 FCM을 직접 발송하지 않는다 | 🟢 활성 (신규) |
| O1 | OCP | 도메인은 어댑터 구현체를 직접 참조하지 않는다 | 🟢 활성 (신규) |
| L1 | LSP | 포트 구현체는 `UnsupportedOperationException`을 던지지 않는다 | 🟢 활성 (신규) |
| L2 | LSP | 서비스는 다른 서비스를 상속하지 않는다 | 🟢 활성 |
| I1 | ISP | 포트 인터페이스의 메서드는 5개 이하 | 🟢 활성 (신규) |
| I2 | ISP | 도메인 간 DTO 교차 참조 금지 | 🔴 위반 1 → Phase 1 |

🟢 즉시 활성 · 🟡 예외 목록과 함께 활성 · 🔴 `@ArchIgnore` 후 해당 Phase에서 활성

---

## 3. DIP — 의존 역전

> 상위 정책은 하위 세부사항에 의존하지 않는다. 둘 다 추상에 의존한다.

이 프로젝트에서 가장 크게 깨져 있는 원칙이다. 도메인 서비스가 S3·Lambda·FCM·FastAPI를 **구체 클래스로** 직접 붙잡고 있어(20개 파일), 도메인 로직만 떼어 테스트할 수 없다.

**D1. `domain`은 `infra` 구현체에 의존하지 않는다.** 외부 자원 접근은 포트 인터페이스를 통한다.

**D2. 엔티티는 DTO에 의존하지 않는다.** 안쪽(도메인)이 바깥쪽(웹)을 알면 API 스펙 변경이 도메인을 흔든다.

**D3. 엔티티는 Spring에 의존하지 않는다.** JPA/Hibernate 애노테이션은 허용한다 — ADR-0001에서 JPA 엔티티와 도메인 모델을 분리하지 않기로 했으므로, 여기까지가 이 프로젝트가 지킬 수 있는 선이다.

현재 위반 2건이고 처리 방식이 다르다.

| 위반 | 처리 |
| --- | --- |
| `User.java:10` — `org.springframework.util.StringUtils.hasText` (147행 1회 사용) | **Phase 1에서 제거.** `nickname != null && !nickname.isBlank()`로 바꾸면 끝난다 |
| `RefreshToken.java` — `@RedisHash`, `@Id`(Spring Data) | **규칙 예외.** Redis 해시 엔티티는 Spring Data Redis 없이 표현할 수 없다. `@RedisHash`가 붙은 클래스는 규칙 대상에서 제외한다 |

**D4. 포트(`*Port`)는 인터페이스여야 한다.** 구현체가 섞이면 포트가 아니다.

포트 시그니처에 쓰이는 값 객체(`PushMessage`, `UploadTarget`)는 포트 계약의 일부라 같은 패키지에 두지만, 그 자체가 포트는 아니므로 D4·I1의 검사 대상이 아니다.

**D5. 도메인은 `RestTemplate`을 직접 쓰지 않는다.** 현재 `AppleService:116`이 메서드 안에서 `new RestTemplate()`을 생성해 스텁 주입 지점 자체가 없다.

**포트 위치 규칙** — 2개 이상 도메인이 쓰면 `global/port`, 단일 도메인 전용이면 `domain/<name>/port`.

---

## 4. SRP — 단일 책임

> 클래스가 변경되는 이유는 하나여야 한다.

기계로 완벽히 검증할 수 없어 **프록시 지표**를 쓴다. 주입 의존성 개수는 그중 가장 신뢰할 만한 신호다.

**S1. 클래스당 주입 의존성 ≤ 8.** 현재 분포:

| 클래스 | 주입 수 | 비고 |
| --- | --- | --- |
| `ActivityRecordServiceV2` | 19 | 예외 등록 |
| `HobbyService` (v1) | 18 | 예외 등록 (528줄) |
| `ActivityRecordService` (v1) | 17 | 예외 등록 |
| `ReactionService` | 12 | 예외 등록 |
| `ActivityService` | 12 | 예외 등록 |
| `AuthService` | 11 | 예외 등록 |
| `UserService` | 10 | 예외 등록 |
| `ActivityRecordServiceV3` | 8 | 통과 |
| `HobbyServiceV2` | 6 | 통과 |

임계값 8은 임의로 정한 값이 아니라 **최근에 작성된 클래스(V3=8, HobbyServiceV2=6)가 통과하는 선**이다. 레거시 7개는 이름으로 명시 예외 처리하고, 예외 목록에 새 클래스를 추가하는 것은 금지한다. 목록은 줄어들기만 해야 한다.

**선택자 주의** — S3와 같은 이유로 `haveSimpleNameEndingWith("Service")`는 쓰지 않는다. `ActivityRecordServiceV2`(19개, 최악의 위반)와 `HobbyServiceV2`가 통째로 빠진다. `haveSimpleNameContaining("Service")`를 쓴다.

**S2. 컨트롤러는 리포지토리에 직접 의존하지 않는다.** 컨트롤러는 매핑과 서비스 위임만 한다 (CLAUDE.md 컨벤션).

**S3. 컨트롤러는 대응 `Docs` 인터페이스를 구현한다.** Swagger 애노테이션이 컨트롤러 본문으로 새는 것을 막는다.

**선택자 주의** — 클래스 이름이 `Controller`로 끝나는지로 고르면 안 된다. 버전 컨트롤러는 `HobbyControllerV2`, `ActivityRecordControllerV2`, `ActivityRecordControllerV3`처럼 **버전 접미사로 끝나서 전부 누락된다.** `@RestController` 애노테이션으로 고른다.

디버그·측정용 컨트롤러 3개(`TestNotificationController`, `global/common/test/controller/TestController`, `TestReactionMeasurementController`)는 Docs 인터페이스가 없고 만들 이유도 없으므로, 이름이 `Test`로 시작하는 클래스를 제외한다. (S1의 레거시 예외 목록과 달리 이건 영구 제외다.)

**S4. 도메인 간 순환 의존이 없다.** 현재 미측정 — 활성화 전에 실제 사이클을 먼저 확인해야 한다.

**S5. `@Transactional` 메서드는 FCM을 직접 발송하지 않는다.** 커밋 전에 발송하면 트랜잭션이 롤백돼도 알림이 이미 나간 뒤다. 정상 경로는 `@TransactionalEventListener(AFTER_COMMIT)` → RabbitMQ 하나뿐이며(`NotificationService#processReactionNotification`), `PushSenderPort`를 트랜잭션 메서드 안에서 직접 호출하는 코드는 전부 위반이다.

대상은 메서드 자신 또는 **선언 클래스**에 `@Transactional`이 붙은 경우다(클래스 레벨 트랜잭션은 현재 `TermsService` 하나뿐). `getMethodCallsFromSelf()`로 직접 호출만 보고, 트랜잭션 메서드가 호출한 다른 메서드 내부의 간접 호출까지는 추적하지 않는다 — "직접 발송하지 않는다"는 규칙 문구와 범위를 맞추기 위한 의도적인 선택이다.

응답 시간 비교 측정에 쓰는 동기 발송 경로(`SyncPushNotificationSender`)는 이 규칙과 부딪히지 않는다. `@Transactional`이 없고, `measure` 프로파일에서만 빈으로 등록돼 프로덕션(`blue`/`green`)에는 존재하지 않는다.

---

## 5. OCP / LSP

> OCP: 확장에는 열리고 수정에는 닫힌다. LSP: 하위 타입은 상위 타입을 대체할 수 있어야 한다.

**O1. 도메인은 어댑터 구현체를 직접 참조하지 않는다.** 어댑터는 `config`에서만 조립한다. 새 저장소·새 푸시 채널을 붙일 때 도메인 코드를 열지 않기 위함이다.

**L1. 포트 구현체는 `UnsupportedOperationException`을 던지지 않는다.** "이 어댑터는 이 메서드를 지원하지 않는다"는 순간 그 포트는 대체 가능하지 않다. 이게 발생하면 포트를 쪼개야 한다는 신호다(→ I1).

**L2. 서비스는 다른 서비스를 상속하지 않는다.** 현재 상속 사용처는 `Notification` 엔티티 계층과 `JwtTokenFilter`, `MyKeyLocator`뿐이다. 버전 간 코드 공유를 상속(`ActivityRecordServiceV3 extends ActivityRecordServiceV2`)으로 해결하려는 유혹을 차단한다 — 그 경우 조합(공유 UseCase 주입)을 쓴다.

---

## 6. ISP — 인터페이스 분리

> 클라이언트가 쓰지 않는 메서드에 의존하게 만들지 않는다.

**I1. 포트 인터페이스의 메서드는 5개 이하.**

Phase 2에서 가장 실수하기 쉬운 지점이다. `S3Service`는 public 메서드가 8개(`generateKey`, `createPresignedPutRequest`, `createFileUrl`, `existsByKey`, `deleteByKey`, `extractKeyFromFileUrl`, `copyObject`, …)인데, 이걸 그대로 `ImageStoragePort` 하나에 옮기면 ISP 위반이다. 이미지 URL만 필요한 `FriendService`가 presigned URL 생성과 삭제까지 의존하게 된다.

역할별로 쪼갠다:

```
ImageUrlPort      # createFileUrl, extractKeyFromFileUrl  — 조회 계열이 사용
ImageUploadPort   # generateKey, createPresignedPutRequest — 업로드 경로만
ImageLifecyclePort# existsByKey, deleteByKey, copyObject   — 생성/삭제 경로만
```

`Docs` 인터페이스와 Spring Data `Repository`는 이 규칙 대상에서 제외한다 (`HobbyControllerDocs`는 1401줄이지만 문서 전용이고, Repository 메서드 수는 Spring Data 관용구다).

**I2. 도메인 간 DTO 교차 참조 금지.** 현재 `record` 컨텍스트의 `ActivityRecord`가 `hobby.dto.request.RecordActivityReqDto`를 참조한다. 컨텍스트 간 통신은 DTO가 아니라 도메인 타입이나 커맨드 객체로 한다.

---

## 7. 기계 검증이 불가능한 규칙 (코드 리뷰 체크리스트)

ArchUnit으로 표현할 수 없어 사람이 봐야 하는 항목이다. **위 규칙과 같은 무게로 취급하지 않는다** — 어겼다고 CI가 막지 않는다.

- **[SRP]** `utils` 클래스에 순수 변환과 외부 I/O를 섞지 않는다.
  현재 `S3Util`이 URL 문자열 변환(`toXxxResizedUrl`) + S3 존재 확인(I/O) + 트랜잭션 동기화 삭제를 한 클래스에 담고 있다. 성격이 다른 셋이다.
- **[OCP]** 새 API 버전은 **기존 서비스를 고치지 않고 새 클래스로** 만든다. 구버전 앱이 v1을 계속 사용한다 (CLAUDE.md).
- **[OCP]** 서비스 안에서 `if (version == 2)` 같은 버전 분기를 두지 않는다. 버전 차이는 클래스 경계로 표현한다.
- **[DIP]** 비즈니스 판단을 `global/`에 두지 않는다. 현재 `AiUserSummaryService.determine(User, Hobby)`가 어댑터 자리에서 도메인 판단을 한다.
- **[ISP]** 포트 메서드가 5개를 넘어가면 개수를 줄이지 말고 **포트를 쪼갠다.** 한 인터페이스에 억지로 욱여넣기 위한 파라미터 플래그(`boolean isThumbnail` 같은)를 만들지 않는다.

---

## 8. ArchUnit 실행 코드

`src/test/java/com/example/ForDay/architecture/ArchitectureTest.java`

```java
package com.example.ForDay.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.example.ForDay.global.port.PushSenderPort;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * 아키텍처 규칙 검증.
 *
 * <p>규칙의 배경과 전문은 {@code docs/architecture-rules.md},
 * 전환 계획은 {@code docs/adr/0001-incremental-hexagonal-architecture.md} 참고.
 *
 * <p>위반이 남아 있는 규칙은 {@link ArchIgnore}로 표시하고 해당 Phase 종료 시 제거한다.
 * JUnit의 {@code @Disabled}는 {@link ArchTest} 필드에 적용되지 않으므로 쓰지 않는다.
 */
@AnalyzeClasses(
        packages = "com.example.ForDay",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ArchitectureTest {

    private static final int MAX_INJECTED_DEPENDENCIES = 8;
    private static final int MAX_PORT_METHODS = 5;

    /**
     * 우리 코드베이스가 작성한 어댑터만 대상으로 한다.
     * 이름만으로 거르면 {@code io.jsonwebtoken.LocatorAdapter} 같은 서드파티 클래스가 걸린다.
     */
    private static final DescribedPredicate<JavaClass> 우리가_만든_어댑터 =
            resideInAPackage("com.example.ForDay..")
                    .and(simpleNameEndingWith("Adapter"))
                    .as("우리 코드베이스의 어댑터 구현체");

    /**
     * 메서드 자신 또는 선언 클래스에 {@code @Transactional}이 붙어 있으면 대상이다.
     * 클래스 레벨 트랜잭션은 현재 {@code TermsService} 하나뿐이지만, 늘어날 수 있어 함께 본다.
     */
    private static final DescribedPredicate<JavaMethod> 트랜잭션_경계_안의_메서드 =
            new DescribedPredicate<>("@Transactional 메서드(또는 그 클래스)") {
                @Override
                public boolean test(JavaMethod method) {
                    return method.isAnnotatedWith(Transactional.class)
                            || method.getOwner().isAnnotatedWith(Transactional.class);
                }
            };

    // ==================== DIP: 의존 역전 ====================

    @ArchTest
    @ArchIgnore(reason = "Phase 2(포트 도입) 완료 후 활성화 - 이슈 #347")
    static final ArchRule D1_도메인은_인프라에_의존하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAPackage("..infra..")
                    .because("외부 자원 접근은 포트 인터페이스를 통한다");

    @ArchTest
    @ArchIgnore(reason = "Phase 1(엔티티 DTO 의존 제거) 완료 후 활성화 - 이슈 #346")
    static final ArchRule D2_엔티티는_DTO에_의존하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..domain..entity..")
                    .should().dependOnClassesThat().resideInAPackage("..dto..")
                    .because("안쪽(도메인)은 바깥쪽(웹)을 알지 않는다");

    @ArchTest
    @ArchIgnore(reason = "Phase 1(User.StringUtils 제거) 완료 후 활성화 - 이슈 #346")
    static final ArchRule D3_엔티티는_스프링에_의존하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..domain..entity..")
                    // Redis 해시 엔티티는 Spring Data Redis 없이 표현 불가 - 영구 예외
                    .and().areNotAnnotatedWith(RedisHash.class)
                    .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                    .because("JPA 애노테이션은 허용하되 스프링 컨테이너는 알지 않는다");

    /**
     * 대상은 <b>포트</b>({@code *Port})로 한정한다.
     *
     * <p>포트 시그니처에 쓰이는 값 객체({@code PushMessage}, {@code UploadTarget})는
     * 포트 계약의 일부라 같은 패키지에 두지만, 그 자체가 포트는 아니므로 검사하지 않는다.
     * 규칙이 막으려는 건 "포트 자리에 구현체가 섞이는 것"이다.
     */
    @ArchTest
    static final ArchRule D4_포트는_인터페이스여야_한다 =
            classes()
                    .that().resideInAPackage("..port..")
                    .and().haveSimpleNameEndingWith("Port")
                    .should().beInterfaces()
                    .because("구현체가 섞이면 포트가 아니다")
                    .allowEmptyShould(true);

    @ArchTest
    @ArchIgnore(reason = "Phase 2(AppleIdentityPort 도입) 완료 후 활성화 - 이슈 #347")
    static final ArchRule D5_도메인은_RestTemplate을_직접_쓰지_않는다 =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().areAssignableTo(RestTemplate.class)
                    .because("외부 HTTP 호출은 어댑터가 담당한다");

    // ==================== SRP: 단일 책임 ====================

    @ArchTest
    static final ArchRule S1_서비스의_주입_의존성이_과하지_않다 =
            classes()
                    .that().resideInAPackage("..service..")
                    // EndingWith가 아니라 Containing - V2/V3 접미사 클래스가 빠지면 안 된다
                    .and().haveSimpleNameContaining("Service")
                    // 아래 7개는 레거시 예외. 목록은 줄어들기만 한다 - 새 클래스 추가 금지.
                    .and().doNotHaveSimpleName("ActivityRecordServiceV2")   // 19
                    .and().doNotHaveSimpleName("HobbyService")              // 18
                    .and().doNotHaveSimpleName("ActivityRecordService")     // 17
                    .and().doNotHaveSimpleName("ReactionService")           // 12
                    .and().doNotHaveSimpleName("ActivityService")           // 12
                    .and().doNotHaveSimpleName("AuthService")               // 11
                    .and().doNotHaveSimpleName("UserService")               // 10
                    .should(주입_의존성이_제한_이하())
                    .because("주입 의존성 개수는 책임 과다의 가장 신뢰할 만한 신호다");

    @ArchTest
    static final ArchRule S2_컨트롤러는_리포지토리를_직접_쓰지_않는다 =
            noClasses()
                    .that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..repository..")
                    .because("컨트롤러는 매핑과 서비스 위임만 한다");

    @ArchTest
    static final ArchRule S3_컨트롤러는_Docs_인터페이스를_구현한다 =
            classes()
                    // 이름이 아니라 애노테이션으로 고른다 - HobbyControllerV2 등이 누락되지 않도록
                    .that().areAnnotatedWith(RestController.class)
                    .and().resideInAPackage("..controller..")
                    // 디버그용 컨트롤러는 영구 제외
                    .and().haveSimpleNameNotStartingWith("Test")
                    .should(Docs_인터페이스를_구현한다())
                    .because("Swagger 애노테이션은 Docs 인터페이스에만 둔다");

    @ArchTest
    @ArchIgnore(reason = "실제 사이클 측정 후 활성화 - Phase 3 이후")
    static final ArchRule S4_도메인_간_순환_의존이_없다 =
            slices()
                    .matching("com.example.ForDay.domain.(*)..")
                    .should().beFreeOfCycles();

    @ArchTest
    static final ArchRule S5_트랜잭션_안에서_푸시를_직접_발송하지_않는다 =
            methods().that(트랜잭션_경계_안의_메서드)
                    .should(푸시를_직접_발송하지_않는다())
                    .because("커밋 전에 발송하면 롤백돼도 알림이 나간다 - AFTER_COMMIT 이벤트 경로를 쓴다");

    // ==================== OCP / LSP ====================

    @ArchTest
    static final ArchRule O1_도메인은_어댑터_구현체를_직접_참조하지_않는다 =
            noClasses()
                    .that().resideInAPackage("..domain..")
                    .and().resideOutsideOfPackage("..config..")
                    .should().dependOnClassesThat(우리가_만든_어댑터)
                    .because("어댑터 조립은 config에서만 한다");

    @ArchTest
    static final ArchRule L1_어댑터는_계약을_거부하지_않는다 =
            noClasses()
                    .that().haveSimpleNameEndingWith("Adapter")
                    .should().accessClassesThat().areAssignableTo(UnsupportedOperationException.class)
                    .because("지원하지 않는 메서드가 있다면 포트를 쪼개야 한다")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule L2_서비스는_다른_서비스를_상속하지_않는다 =
            classes()
                    .that().resideInAPackage("..service..")
                    .should(다른_서비스를_상속하지_않는다())
                    .because("버전 간 코드 공유는 상속이 아니라 조합(공유 UseCase 주입)으로 한다");

    // ==================== ISP: 인터페이스 분리 ====================

    /**
     * 대상은 <b>포트 인터페이스</b>로 한정한다(D4와 동일 기준).
     *
     * <p>값 객체는 컴포넌트 수만큼 접근자가 생겨 메서드 수가 쉽게 5개를 넘지만,
     * 이 규칙이 재는 건 "클라이언트가 의존하게 되는 포트의 크기"다.
     */
    @ArchTest
    static final ArchRule I1_포트는_작게_유지한다 =
            classes()
                    .that().resideInAPackage("..port..")
                    .and().haveSimpleNameEndingWith("Port")
                    .should(메서드가_제한_이하())
                    .because("클라이언트가 쓰지 않는 메서드에 의존하게 만들지 않는다")
                    .allowEmptyShould(true);

    /**
     * 대상은 <b>요청</b> DTO로 한정한다.
     *
     * <p>응답 DTO까지 막으면 ADR-0001이 의도적으로 남겨둔 조회 경로 타협과 충돌한다.
     * {@code ActivityRecordRepositoryImpl.getStickerInfo}는 QueryDSL Projections로
     * {@code hobby}의 응답 DTO를 바로 만드는데, 이는 "명령은 유스케이스 경유,
     * 조회는 현행 유지"라는 결정에 따라 허용된 경우다.
     */
    @ArchTest
    @ArchIgnore(reason = "Phase 1(ActivityRecord의 hobby.dto 참조 제거) 완료 후 활성화 - 이슈 #346")
    static final ArchRule I2_도메인_간_요청DTO_교차_참조_금지 =
            noClasses()
                    .that().resideInAPackage("..domain.record..")
                    .should().dependOnClassesThat().resideInAPackage("..domain.hobby.dto.request..")
                    .because("컨텍스트 간 통신은 요청 DTO가 아니라 도메인 타입으로 한다");

    // ==================== 커스텀 조건 ====================

    private static ArchCondition<JavaClass> 주입_의존성이_제한_이하() {
        return new ArchCondition<>("주입 의존성이 " + MAX_INJECTED_DEPENDENCIES + "개 이하여야 한다") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                long count = item.getFields().stream()
                        .filter(f -> f.getModifiers().contains(JavaModifier.FINAL))
                        .filter(f -> !f.getModifiers().contains(JavaModifier.STATIC))
                        .count();
                if (count > MAX_INJECTED_DEPENDENCIES) {
                    events.add(SimpleConditionEvent.violated(item, String.format(
                            "%s 의 주입 의존성이 %d개다 (최대 %d개) - 책임을 쪼갤 것",
                            item.getName(), count, MAX_INJECTED_DEPENDENCIES)));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> 메서드가_제한_이하() {
        return new ArchCondition<>("메서드가 " + MAX_PORT_METHODS + "개 이하여야 한다") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                int count = item.getMethods().size();
                if (count > MAX_PORT_METHODS) {
                    events.add(SimpleConditionEvent.violated(item, String.format(
                            "%s 의 메서드가 %d개다 (최대 %d개) - 역할별로 포트를 분리할 것",
                            item.getName(), count, MAX_PORT_METHODS)));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> Docs_인터페이스를_구현한다() {
        return new ArchCondition<>("대응 Docs 인터페이스를 구현해야 한다") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean implemented = item.getAllRawInterfaces().stream()
                        .anyMatch(i -> i.getSimpleName().endsWith("Docs"));
                if (!implemented) {
                    events.add(SimpleConditionEvent.violated(item,
                            item.getName() + " 에 대응하는 Docs 인터페이스가 없다"));
                }
            }
        };
    }

    /**
     * 직접 호출만 본다({@code getMethodCallsFromSelf} — 트랜잭션 메서드가 호출한 다른 메서드
     * 내부에서 일어나는 간접 호출까지는 추적하지 않는다). 규칙 문구("직접 발송하지 않는다")와
     * 정확히 일치시키기 위한 의도적인 범위다.
     */
    private static ArchCondition<JavaMethod> 푸시를_직접_발송하지_않는다() {
        return new ArchCondition<>("PushSenderPort를 직접 호출하지 않아야 한다") {
            @Override
            public void check(JavaMethod item, ConditionEvents events) {
                for (JavaMethodCall call : item.getMethodCallsFromSelf()) {
                    if (call.getTargetOwner().isAssignableTo(PushSenderPort.class)) {
                        events.add(SimpleConditionEvent.violated(item, String.format(
                                "%s 가 트랜잭션 안에서 %s 를 직접 호출한다 - AFTER_COMMIT 이벤트로 바꿀 것",
                                item.getFullName(), call.getTarget().getFullName())));
                    }
                }
            }
        };
    }

    private static ArchCondition<JavaClass> 다른_서비스를_상속하지_않는다() {
        return new ArchCondition<>("다른 서비스를 상속하지 않는다") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getRawSuperclass().ifPresent(parent -> {
                    if (parent.getSimpleName().endsWith("Service")) {
                        events.add(SimpleConditionEvent.violated(item, String.format(
                                "%s 가 %s 를 상속한다 - 조합으로 바꿀 것",
                                item.getName(), parent.getName())));
                    }
                });
            }
        };
    }
}
```

---

## 9. 규칙 변경 절차

- 규칙 **추가**: PR에 근거(어떤 사고/중복을 막는지)를 적는다.
- 규칙 **완화·예외 추가**: ADR에 기록한다. S1 예외 목록에 새 클래스를 넣는 것은 규칙 완화에 해당한다.
- `@ArchIgnore` 제거는 해당 Phase의 완료 조건이다. Phase가 끝났는데 애노테이션이 남아 있으면 그 Phase는 끝난 것이 아니다.
