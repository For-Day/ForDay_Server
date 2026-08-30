package com.example.ForDay.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameEndingWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
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
