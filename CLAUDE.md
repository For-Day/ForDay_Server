# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트

ForDay (`ForDay_Server`) — 취미 습관 앱("66일 동안 취미 채우기")의 Spring Boot 3.5 / Java 17 백엔드. 커밋, PR, 에러 메시지, Swagger 문서는 모두 한국어로 작성되어 있으므로 계속 한국어로 작성한다.

## 명령어

```bash
./gradlew build                 # 컴파일 + 테스트 + jar (QueryDSL Q클래스를 build/generated에 생성)
./gradlew build -x test         # CI가 Docker 빌드 전에 실행하는 명령
./gradlew bootRun               # 로컬 실행 (profile `local`)
./gradlew test                  # 전체 테스트
./gradlew test --tests "HobbyServiceV2Test"                                # 단일 클래스
./gradlew test --tests "*.HobbyServiceV2Test*MyHobbySettingDeletableTest"  # 단일 @Nested 클래스
```

로컬 실행에는 MySQL(3306의 `forday` DB)과 Redis(6379)가 필요하다. RabbitMQ와 FCM 푸시는 해당 기능을 실제로 호출할 때만 사용된다. 테스트는 `@ActiveProfiles("test")`로 H2 인메모리(`src/test/resources/application-test.yml`)를 쓰지만, `@SpringBootTest` 테스트는 여전히 **실제 로컬 Redis에 접속**하므로 Redis 없이는 통합 테스트가 실패한다. QueryDSL Q클래스는 애노테이션 프로세서가 생성하므로, `@Entity`를 수정한 뒤에는 새 `Q*` 필드를 참조하기 전에 빌드를 다시 해야 한다.

`@Profile("local")`인 `DataInitializer` / `NotificationDataInitializer` / `ReactionInitializer`가 `ApplicationReadyEvent` 시점에 더미 데이터를 넣는다. `local` 외의 프로파일에서는 절대 실행되지 않는다.

Swagger UI: `/swagger-ui/index.html`.

## 아키텍처

`com.example.ForDay`는 크게 세 영역으로 나뉜다.

- **`domain/<name>/`** — 바운디드 컨텍스트별 패키지(`activity`, `app`, `auth`, `friend`, `hobby`, `notification`, `reaction`, `recent`, `record`, `term`, `user`). 각각 `controller / dto{request,response} / entity / repository / service / type / utils` 구조를 가진다. `utils`에는 공통 조회·검증 헬퍼(`ActivityRecordUtil`, `HobbyUtil`), `validator`에는 규칙 검사(`HobbyValidator`), `type`에는 enum이 들어간다.
- **`global/`** — 횡단 관심사: `common`(응답 래퍼, `ErrorCode`, 성공 코드, `BaseTimeEntity`, 상수), `config`(security, redis, querydsl, swagger, schedule, restTemplate), `filter/JwtTokenFilter`, `oauth/CustomUserDetails`, `firebase`(FCM), `rabbitmq`, `ai`, `util`.
- **`infra/`** — AWS 어댑터: `s3`(이미지 업로드 / URL 변환), `lambda`(`CoverLambdaInvoker`, 취미 커버 생성용 동기 invoke).

### API 버저닝

기존 엔드포인트는 접두사 없는 경로(`/hobbies`, `/records`, `/users` 등)에 있고, 이후 버전은 `/api/v2/...`, `/api/v3/...` 아래에 `controller/vN` + `service/vN` 패키지를 병행해서 둔다(`ActivityRecordServiceV3`, `HobbyServiceV2`). 새 버전은 기존 클래스를 고치는 게 아니라 **옆에 새 클래스를 만들고**, 버전 접미사가 붙은 DTO(`GetRecordDetailResDtoV3`)를 추가하는 방식이다. 구버전 앱이 계속 쓰므로 v1 서비스를 새 응답 형태로 갈아엎지 말 것.

### 컨트롤러 ⇄ Docs 인터페이스

모든 컨트롤러는 같은 위치의 `XxxControllerDocs` 인터페이스를 구현하며, Swagger 애노테이션(`@Tag`, `@Operation`, `ErrorCode` 이름이 담긴 실제 JSON `@ExampleObject`를 포함한 `@ApiResponses`)은 **전부 그 인터페이스에** 둔다. 컨트롤러는 매핑과 서비스 위임만 한다. 엔드포인트를 추가할 때는 양쪽 모두에 메서드를 추가하고, Docs 인터페이스에 실제로 발생 가능한 에러 코드를 나열한다.

### 응답 / 예외 래핑

`GlobalResponseAdvice`(`ResponseBodyAdvice`)가 모든 2xx 응답을 `{status, success:true, data}`로 감싼다. 따라서 서비스와 컨트롤러는 순수 DTO만 반환하고 `GlobalResponse`를 직접 만들지 않는다(Swagger 경로와 `String` 본문은 제외). 실패는 `new CustomException(ErrorCode.X)`를 던지면 `GlobalExceptionHandler`가 `{status, success:false, data:{errorClassName, message}}`로 변환한다. `ErrorCode`는 도메인별로 묶인 단일 enum으로 HTTP 상태와 사용자용 한국어 메시지를 함께 갖는다. 임의 예외를 던지지 말고 여기에 추가할 것.

### 인증

무상태 JWT. `JwtTokenFilter`가 `Bearer` 토큰을 파싱해 `socialId`로 `User`를 조회하고 `CustomUserDetails`를 SecurityContext에 넣는다. 컨트롤러는 `@AuthenticationPrincipal CustomUserDetails user`로 받고, 서비스에서는 `UserUtil.getCurrentUser(...)`로 꺼낸다. 공개 경로는 `SecurityConfig`에 화이트리스트로 지정되어 있고(헬스체크, swagger, `/auth/**` 소셜 로그인, 약관, 앱 메타데이터) 나머지는 전부 `authenticated()`다. 권한은 `GUEST`/`USER`이며, 권한 검사는 필터 체인이 아니라 서비스 코드에서 수행한다(`SecurityConfig`의 `hasRole` 매처는 주석 처리됨).

### 영속성

Spring Data JPA + QueryDSL. 복잡한 조회는 `XxxRepository` / `XxxRepositoryCustom` / `XxxRepositoryImpl` 3종 구조에서 `Projections`로 조회 DTO에 매핑한다. 엔티티는 `BaseTimeEntity`를 상속해 `createdAt`/`updatedAt`을 `Asia/Seoul` 기준으로 기록한다(애플리케이션 시작 시 기본 타임존도 `Asia/Seoul`로 강제한다). 소프트 삭제는 `deleted` 불리언 + `@SQLRestriction("deleted = false")` 조합이다(`Hobby` 참고). 필터링된 행은 JPQL/QueryDSL에서 아예 보이지 않으므로 접근하려면 네이티브 쿼리가 필요하다. `User.id`는 UUID `CHAR(36)`이고 나머지 대부분은 `Long` identity다. `ddl-auto: update`라 스키마는 엔티티를 따라가며, 별도 마이그레이션 도구는 없다.

### Redis

같은 인스턴스를 두 가지 용도로 쓴다.

1. `RedisCacheConfig`를 통한 `@Cacheable`(기본 TTL 3분, JSON 직렬화. 페이지네이션 결과를 왕복시키기 위해 `SliceImpl`을 서브타입으로 등록). 캐시 이름과 수동 무효화용 키 패턴은 `CacheConstants`에 모여 있으므로, 캐시된 데이터를 무효화하는 로직은 이 패턴을 사용해서 evict한다.
2. 리액션 쓰기 경로의 순수 `RedisTemplate` 사용: `ReactionService`가 `recordId:userId:type`을 `reaction_queue` 리스트에 push하고 `reaction:done:*` 키를 남기면, `ReactionScheduler`가 1초마다 최대 1000건씩 MySQL로 내려쓰고, `ReactionRankingService` / `ReactionRankingBatchService`가 랭킹 상태를 관리한다. "내가 리액션했는지" 조회가 DB뿐 아니라 Redis도 참조하므로 리액션 관련 코드를 건드릴 때는 양쪽 정합성을 함께 맞춰야 한다. 이 경로의 부하 테스트 스크립트는 `scripts/k6/`에 있다.

### 알림

인앱 알림 저장과 푸시 발송이 분리되어 있다. 서비스가 `@TransactionalEventListener(AFTER_COMMIT)`(`NotificationEventListener`)로 `NotificationEventDto`를 발행 → RabbitMQ 토픽 익스체인지(`notification.exchange`) → `NotificationConsumer`가 알림을 저장하고 기기 토큰별로 FCM을 발송하며, 개별 토큰 실패는 로그만 남기고 계속 진행한다. 트랜잭션 서비스 메서드 안에서 FCM을 직접 발송하지 말 것.

### AI

`global/ai/*Service`는 외부 FastAPI 서비스(`fastapi.url`)를 `RestTemplate`으로 호출한다(yml에 OpenAI 설정도 존재). `AiCallCountService`가 사용자별 `ai.max-call-limit`을 강제한다. AI 실패는 `AI_*` 에러 코드(`AI_RESPONSE_INVALID`, `AI_SERVICE_ERROR`, `AI_RATE_LIMIT_EXCEEDED`)로 매핑된다.

## 테스트 컨벤션

두 가지 스타일을 쓴다. `@ExtendWith(MockitoExtension.class)` + `@InjectMocks`/`@Mock` + BDDMockito 기반의 빠른 단위 테스트, 그리고 리포지토리/QueryDSL·동시성 테스트를 위한 `@SpringBootTest @ActiveProfiles("test")`(대개 `@Transactional`). 시나리오는 한국어 `@DisplayName`을 붙인 `@Nested` 클래스로 표현하고, 단언은 AssertJ를 쓴다.

## 배포

`main`에 push하면 `.github/workflows/deploy.yml`이 실행된다: 빌드(`-x test`) → Docker 이미지 → ECR → EC2 SSH → **블루-그린** 전환. 워크플로가 `application.yml`과 Firebase admin JSON을 GitHub Secrets에서 주입하고, `blue`/`green` 프로파일과 8080/8081 포트를 번갈아 쓰며(프로파일 그룹은 `application-prod.yml`에 정의), `/health_check`를 5초 간격 20회 폴링한 뒤 nginx의 `service-env.inc`를 전환하고 이전 컨테이너를 제거한다. 브랜치 흐름은 `feat/*` → `dev` → `release` → `main`.

## 컨벤션 (README 기준)

- 이슈 `[prefix] 작업내용`, 브랜치 `prefix/#이슈번호-설명`, 커밋 `[#이슈번호] prefix: 작업내용`.
- prefix: feat, fix, chore, bug, hotfix, refactor, docs, test, setting, deploy.
- DTO 네이밍: `XxxReqDto` / `XxxResDto`, 엔드포인트 버전이 갈리면 버전 접미사(`...ReqDtoV2`)를 붙인다.
