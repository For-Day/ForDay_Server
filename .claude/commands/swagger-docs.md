# 스웨거 문서화 스킬

지정한 Controller의 `XxxControllerDocs` 인터페이스를 작성하거나 갱신한다.

인자: `$ARGUMENTS` — 대상 Controller 파일명 또는 경로 (예: `HobbyControllerV2` 또는 전체 경로)

## 실행 순서

1. `$ARGUMENTS`로 대상 `XxxController.java`를 찾는다. 파일명만 주어졌다면 Glob으로 실제 경로를 찾는다. 못 찾으면 사용자에게 경로를 확인한다.
   - 버전이 갈린 컨트롤러(`controller/v1`, `controller/v2`, `controller/v3`)가 여러 개면 어느 버전인지 확인한다. 다른 버전 파일은 건드리지 않는다.
2. Controller를 읽고 각 핸들러 메서드에서 다음을 파악한다:
   - HTTP 메서드/경로 (`@GetMapping`, `@PostMapping` 등 + 클래스의 `@RequestMapping`)
   - 요청/응답 타입 — 요청은 `@RequestBody XxxReqDto` / `@ModelAttribute` / `@RequestParam`, 응답은 서비스가 반환하는 `XxxResDto` (컨트롤러는 `GlobalResponse`로 감싸지 않고 DTO를 그대로 반환한다)
   - 인증 필요 여부 — `@AuthenticationPrincipal CustomUserDetails` 파라미터가 있으면 인증 필요. `SecurityConfig`의 `permitAll` 화이트리스트에 경로가 있으면 인증 불필요.
3. 해당 엔드포인트가 실제로 던질 수 있는 에러 코드를 수집한다.
   - 에러 코드는 도메인별 파일이 아니라 전역 단일 enum인 `global/common/error/exception/ErrorCode.java`에 도메인별 주석으로 묶여 있다. 여기서 코드명·HTTP 상태·한국어 메시지를 그대로 가져온다.
   - 컨트롤러가 호출하는 서비스(및 `utils`/`validator` 클래스)를 따라가며 실제로 `throw new CustomException(ErrorCode.X)`가 발생하는 코드만 수집한다.
   - `ErrorCode` enum에 없는 코드를 지어내지 않는다. 필요한 코드가 없으면 사용자에게 먼저 확인한다.
   - `@Valid` 검증이 걸린 요청 DTO가 있으면 `MethodArgumentNotValidException` → `VALIDATION_ERROR` 400 케이스도 함께 문서화한다.
4. 같은 도메인의 기존 Docs 인터페이스(예: `domain/record/controller/v3/ActivityRecordControllerV3Docs.java`)를 레퍼런스 패턴으로 삼아 다음 스타일을 그대로 따라 `XxxControllerDocs.java`를 작성한다:
   - 인터페이스 레벨 `@Tag(name, description)` — description은 한국어로 (예: `"활동 기록 관련 API V3"`)
   - 메서드별 `@Operation(summary, description)` — 모두 한국어로 작성하고, 파라미터에 따라 동작이 달라지는 조건·제약을 description에 적는다.
   - `@ApiResponses`에 성공 케이스와 실제 발생 가능한 에러 케이스를 모두 나열한다.
     - 성공: `@Content(schema = @Schema(implementation = XxxResDto.class))`
     - 에러: `@ExampleObject(name = "ERROR_CODE_NAME", value = ...)`로 아래 JSON을 하드코딩한다. 응답은 `GlobalExceptionHandler`가 만드는 형태를 따른다:
       ```json
       {"status": 400, "success": false, "data": {"errorClassName": "HOBBY_ID_REQUIRED", "message": "특정 취미 소식 조회 시 취미 ID는 필수입니다."}}
       ```
       `status`, `errorClassName`, `message`는 `ErrorCode` enum 값과 정확히 일치시킨다. 같은 상태 코드에 여러 에러가 있으면 `@ExampleObject`를 여러 개 나열한다.
   - 인증이 필요 없는 API에는 `@SecurityRequirements`를 붙인다 (전역 `SwaggerConfig`가 모든 API에 `BearerAuth`를 요구하도록 설정되어 있음).
   - `@AuthenticationPrincipal CustomUserDetails`처럼 내부 주입되는 파라미터는 `@Parameter(hidden = true)`로 숨긴다.
5. Controller가 아직 Docs 인터페이스를 `implements`하지 않았다면 `implements XxxControllerDocs`를 추가하고, 각 메서드에 `@Override`를 붙인다. Swagger 애노테이션은 Controller가 아니라 **Docs 인터페이스에만** 둔다.
6. 작성 결과를 요약해 보여준다 (어떤 에러 코드를 어디서 가져왔는지 포함). 파일 변경은 git으로 되돌릴 수 있으므로 바로 저장하되, 임의로 지어낸 부분이 있다면 반드시 짚어서 알린다.

## 규칙

- Docs 인터페이스 파일은 Controller와 같은 디렉토리(`domain/<도메인>/controller[/vN]/`)에 둔다.
- 엔티티, DTO, 서비스의 비즈니스 로직은 건드리지 않는다 — Docs 인터페이스 작성과 Controller의 `implements`/`@Override` 추가만 한다.
- 이미 `XxxControllerDocs`가 존재하면 새로 만들지 않고 기존 내용을 갱신한다 (누락된 메서드 추가, 실제와 어긋난 설명 수정).
- 에러 코드/메시지는 항상 실제 `ErrorCode` enum 값을 근거로 작성한다. 추측 금지.

## 실행

위 순서대로 바로 실행한다.
