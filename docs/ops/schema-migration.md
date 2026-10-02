# 스키마 변경 관리 (Flyway)

운영 스키마는 `src/main/resources/db/migration`의 Flyway 마이그레이션이 정본이다.
Hibernate는 스키마를 만들지도 검증하지도 않는다(`ddl-auto: none`).

## 왜 이렇게 바꿨나

전에는 운영까지 `ddl-auto: update`였다. 스키마에 선언된 정의가 없고, 현재 모습이
"배포된 엔티티 코드의 누적 + 손으로 친 DDL"의 결과로만 존재했다. 그 때문에 사고가
두 건 있었다.

1. **덤프 복원 실패** — k8s 컷오버 중 운영 덤프를 새 MySQL에 복원하다 `ERROR 3780`
   (FK 컬럼 collation 불일치)으로 실패했다. Hibernate가 최초 부팅 때 자동 생성한
   빈 스키마가 운영 스키마와 같지 않았다. (`ForDay_GitOps/README.md`)
2. **아웃박스 적재 100% 실패** — `@Lob String payload`에 length가 없어 Hibernate 6이
   기본 255를 적용하고 MySQL에서 `tinytext`로 생성됐다. 실제 payload는 약 399바이트라
   `Data too long for column 'payload'`로 전부 실패했고, 아웃박스 저장이 반응
   트랜잭션 안에 있어 반응 API가 500을 반환했다. (`docs/perf/notification-pipeline.md`)

`ddl-auto: update`는 **더하기만 한다.** 컬럼 추가는 하지만 기존 컬럼의 타입은 바꾸지
않고 삭제도 하지 않는다. 그래서 2번은 `ALTER TABLE`을 손으로 쳐야 했고, 그 ALTER는
자바 주석에만 기록이 남았다. 그리고 스키마 변경이 PR diff에 보이지 않는다 - 엔티티에
필드를 추가하는 PR의 diff에는 자바 코드만 나오고, 그 배포가 운영 DB에 `ALTER TABLE`을
실행한다는 사실이 어디에도 나타나지 않는다.

## 규칙

- **마이그레이션은 더하기만 한다(expand/contract).** blue/green 승격 구간에는 신 파드가
  마이그레이션을 끝낸 뒤에도 승격 전까지 구 파드가 트래픽을 받는다. 그 동안 구 코드가
  신 스키마를 본다. 컬럼 추가는 안전하지만 **삭제·이름 변경·타입 축소는 그 구간에 즉시
  장애**가 된다. 제거는 코드가 먼저 안 쓰게 된 다음 배포에서 별도 버전으로 한다.
- **이미 적용된 마이그레이션 파일은 수정하지 않는다.** 체크섬이 어긋나면 Flyway가 기동을
  거부한다. 고칠 일이 있으면 다음 버전을 새로 쓴다.
- **되돌릴 때도 앞으로 간다.** Flyway 무료판에는 `undo`가 없다. 역방향 `V<n+1>`을 쓴다.
- **엔티티 선언과 마이그레이션은 함께 커밋한다.** 한쪽만 바꾸면 CI의 `migration` job이
  막는다(실제로 한 번 막았다 - 아래 참고).
- **스키마를 "더 좋게" 재설계하는 변경은 별도 버전으로 분리한다.** 인덱스 추가나 정규화를
  기능 변경과 같은 버전에 섞지 않는다.

## 프로파일별 설정

| 프로파일 | `ddl-auto` | Flyway | 이유 |
| --- | --- | --- | --- |
| `common` (운영 blue/green) | `none` | 활성 | Hibernate가 스키마에 관여하지 않는다 |
| `local` | `validate` | 활성 | 개발자가 불일치를 먼저 발견한다 |
| `test` (H2) | `create` | 비활성 | `db/migration`은 MySQL 전용 DDL이라 H2에 적용 불가 |
| `migration` (CI 전용) | `validate` | 활성 | 마이그레이션 적용 + 엔티티 대조 검증 |

운영을 `validate`가 아니라 `none`으로 둔 이유: 앱 레플리카가 1개라
(`ForDay_GitOps/charts/forday-app/values.yaml`) 검증 실패가 곧 파드 기동 실패이고
그게 전면 중단이 된다. 엔티티와 스키마의 불일치는 **배포 전에 알아야 하는 정보**이므로
CI에서 잡고, 운영 기동 경로에서는 그 실패 가능성을 뺀다.

## 검증 장치

- `.github/workflows/test.yml`의 `migration` job - MySQL 8 서비스 컨테이너에
  마이그레이션을 적용하고 `ddl-auto: validate`로 컨텍스트를 띄운다. 기존 H2 `test` job과
  병렬 실행된다.
- `MigrationVerificationTest` - 마이그레이션 전건 성공 / 기준선 이후 마이그레이션 존재 /
  `payload`가 `longtext`(V2 반영) / DB 기본 collation이 테이블들과 일치(V3 반영).
- 로컬 실행: `./gradlew migrationTest`. localhost:3306에 MySQL 8이 필요하고, 포트가
  점유돼 있으면 `MIGRATION_DB_PORT` 등으로 덮어쓴다(`application-migration.yml`).

## 마이그레이션 목록

| 버전 | 내용 |
| --- | --- |
| `V1__baseline.sql` | 2026-10-01 운영 스키마 기준선. 테이블 24 / 컬럼 206 / FK 25 / 인덱스 54 |
| `V2__outbox_payload_to_longtext.sql` | `notification_outbox.payload` `tinytext` -> `longtext` |
| `V3__align_database_default_collation.sql` | DB 기본 collation을 테이블들과 정렬 |

### V1 기준선에서 덤프와 달리 한 것

- `AUTO_INCREMENT=N` 테이블 옵션 제거 - 스키마가 아니라 그 시점의 카운터 값이라, 빈 DB에
  적용한 결과와 운영을 비교할 때 매번 어긋난다.
- mysqldump의 `/*!...*/` 조건부 주석 래퍼 제거. 대신 `FOREIGN_KEY_CHECKS`를 평문으로
  껐다 - 테이블이 알파벳순이라 아직 만들어지지 않은 테이블을 참조하는 FK가 있다.
- 제약명(`FKjkwpuymqnqrn0a6kos0kaaf1h` 등)은 Hibernate 생성 이름을 그대로 유지한다.
  운영과 같은 이름이어야 비교가 성립한다.

## 측정 기록

### 기준선 수집 (2026-10-01, 운영 읽기 전용)

| 항목 | 값 |
| --- | --- |
| 테이블 / 컬럼 / FK / 인덱스 | 24 / 206 / 25 / 54 |
| `@Entity` 26개 ↔ 테이블 24개 | 차이 0 (둘은 `SINGLE_TABLE` 서브클래스) - 고아 테이블 0건 |
| MySQL 버전 | 8.0.37 |
| `notification_outbox.payload` | `tinytext`, 255바이트, 테이블 0행 |
| collation (서버 / DB / 테이블·컬럼) | `utf8mb3_general_ci` / `utf8mb4_unicode_ci` / `utf8mb4_0900_ai_ci` (24-24, 74-74) |

### V1 정확성 (로컬, `bitnamilegacy/mysql:8.0.37` + 운영과 같은 DB 기본값)

| 항목 | 값 |
| --- | --- |
| V1 적용 결과 ↔ 운영 덤프 (정규화 후 diff) | **차이 0줄** (양쪽 334줄) |
| V1 적용 후 규모 | 24 / 206 / 25 - 운영과 일치 |

### collation 잠복 위험 재현

| 단계 | 결과 |
| --- | --- |
| SQL: collation 미지정 + `users` FK 테이블 생성 | `ERROR 3780` - 컷오버 실패와 동일 |
| Hibernate: `users` 참조 임시 엔티티 + `ddl-auto: update` | `create table ... engine=InnoDB` (charset 절 없음) |
| 생성된 테이블 collation | `utf8mb4_unicode_ci` (DB 기본값 상속) |
| FK 추가 | 실패. 단 `WARN`만(`ExceptionHandlerLoggedImpl`), FK 관련 `ERROR` 0건 |
| 앱 기동 | **성공** (29.8초) |
| 최종 상태 | 테이블은 생성, **FK 제약은 존재하지 않음** |

예상은 "다음 배포가 실패한다"였으나 실제는 **배포가 성공하면서 제약만 조용히 빠지는**
양상이었다. 참조 정합성이 소리 없이 사라지고 발견 시점은 데이터가 깨진 뒤가 된다.
V3 적용 후 같은 테스트를 다시 하니 성공하고 FK가 생성됐다.

### 장애 주입

| 주입 | 결과 |
| --- | --- |
| 엔티티에만 컬럼 추가 (마이그레이션 없이) | `Schema-validation: missing column [ci_probe] in table [notification_outbox]` -> 기동 거부 (53초) |
| 이미 적용된 V2 파일 수정 | `Migration checksum mismatch for migration version 2` -> 기동 거부 |

### 운영 적용

| 항목 | 값 |
| --- | --- |
| V2 수동 적용 (`ALTER TABLE ... MODIFY payload LONGTEXT`) | 659ms, 적용 시점 테이블 0행 |
| V3 수동 적용 (`ALTER DATABASE ... COLLATE`) | 481ms, 메타데이터만 |
| 적용 후 운영 스키마 ↔ V1+V2+V3 | **차이 0줄** |

Flyway 배선보다 운영 버그가 먼저라고 판단해 두 DDL을 수동으로 먼저 적용했다. 두
마이그레이션은 멱등해서, 이후 Flyway가 baseline(V1) 등록 후 재실행해도 스키마가 바뀌지
않는다.

### 배포 (2026-10-02, PR #413)

| 항목 | 값 |
| --- | --- |
| 머지 -> blue/green 승격 완료 | 약 6분 |
| Flyway 실행 | **0.050초** (baseline 0ms + V2 28ms + V3 22ms) |
| 앱 기동 | 40.19초 |
| 배포 중 5xx | 0건 (구 파드가 승격까지 서비스) |
| 적용 후 스키마 규모 | 24 / 206 / 25 - 불변 |
| Hibernate DDL 로그 | 0건 (`ddl-auto: none`이 실제로 동작) |
| CI 전체 소요 | 2분 54초 -> 3분 14초 (**+20초**, MySQL 컨테이너 추가분) |

운영 `flyway_schema_history`: `BASELINE(v1)` + `SQL(v2)` + `SQL(v3)`, 전건 `success=1`.

### CI가 실제로 막은 사례

Flyway 커밋만 분리해 릴리즈하려 했을 때, `columnDefinition = "LONGTEXT"` 엔티티 선언이
다른 커밋에 섞여 있어 **스키마는 `longtext`인데 엔티티는 `tinytext`를 기대하는** 상태가
됐다. `migration` job이 잡았다.

```
Schema-validation: wrong column type encountered in column [payload]
in table [notification_outbox]; found [longtext], but expecting [tinytext]
```

이 job이 없었다면 운영은 `ddl-auto: none`이라 Hibernate가 검증하지 않으므로 **기동은
성공하고 런타임에 가서야 드러났을 것이다.**

## 범위 밖으로 둔 것

- **서버 기본 collation** - `collation_server = utf8mb3_general_ci`. MySQL 차트 설정
  영역이라(`ForDay_GitOps/apps/mysql.yaml`) 다루지 않았다. DB 기본값이 맞아 `CREATE TABLE`
  상속 경로는 해결됐지만, **새 DB를 만들면 같은 문제가 재발한다.**
- **MongoDB `auto-index-creation: true`** - `application.yml`이 "RDB의 `ddl-auto:update`와
  같은 목적"이라고 적어 둔 항목. 같은 성질이지만 Mongo는 스키마리스라 관리 대상이 인덱스
  뿐이고 타입 변경 같은 파괴적 변경이 없다. Flyway도 MongoDB를 다루지 않는다. **인덱스가
  늘어나면 재검토한다.**
- **Flyway 실행을 initContainer로 분리** - 현재는 앱 기동 시 실행된다. 실패하면 컨텍스트
  기동이 실패해 파드가 Ready에 도달하지 못하고, Rollout이 승격하지 않아 구 파드가 계속
  서비스한다(안전한 실패 모드). initContainer로 옮기면 "구 코드가 신 스키마를 보는"
  구간을 더 줄일 수 있으나, 그 구간은 위 expand/contract 규칙으로 덮고 있다.
