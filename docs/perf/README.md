# 부하 테스트 측정 환경 (이슈 #374)

이 문서는 운영과 분리된 부하 테스트 전용 환경을 어떻게 만들고, 어떤 규칙으로 측정하는지
담는다. 반응(reaction) API 4단계 재측정(`#375`)과 스파이크/포화점 측정(`#376`)이 이 환경 위에서
돈다. Actuator/Prometheus 지표 노출(`#373`)은 이미 돼 있어 이 환경에서도 그대로 쓴다 —
자세한 지표 목록은 [`docs/perf/metrics.md`](./metrics.md) 참고.

## 왜 운영 EC2에서 측정하면 안 되는가

- 운영 EC2는 Redis·RabbitMQ가 앱과 같은 `forday-net` 도커 네트워크에 함께 떠 있어, 부하가 서비스 전체로 번진다
- 스토어에 출시된 서비스라 실제 사용자에게 장애가 발생한다
- blue-green이 같은 인스턴스를 쓰므로, 측정 중 `main` 푸시가 일어나면 컨테이너 교체로 측정이 오염된다
- 로컬(가정용 인터넷)에서 EC2로 부하를 쏘면 서버 한계가 아니라 내 업로드 대역폭·왕복 지연을 측정하게 된다

## 환경 구성

| 구성 요소 | 내용 |
| --- | --- |
| 측정 대상 EC2 | 앱(`measure` 프로파일) + Redis + RabbitMQ, 운영과 동일 배치 |
| 측정용 RDS | 운영과 분리된 별도 소규모 MySQL 인스턴스 |
| 부하 발생기 EC2 | 같은 리전·VPC, k6 설치 |
| Terraform | `ForDay_Infra`의 `perf_test.tf` — 콘솔이 아니라 이 저장소가 직접 생성/관리(측정 종료 후 `terraform destroy`로 정리하기 위함) |

실제 기동 후 확인한 값(2026-09-17 측정 기준):

| 항목 | 값 |
| --- | --- |
| 측정 대상 인스턴스 타입 | `t3.small` — 운영 EC2(`i-03a8bda067b2b14ae`)와 동일 타입·동일 AMI(`ami-0130d8d35bcd2d433`) |
| k6 발생기 인스턴스 타입 | `t3.small` |
| JVM 옵션 | 명시적 튜닝 없음(`java -jar app.jar` 기본값) — 힙 크기 등 JVM 옵션 자체가 병목 요인이었는지는 이번 라운드에서 별도로 확인하지 않았다 |
| MySQL 버전 | `8.0.43` — 운영 RDS(`forday-rds`)와 동일 엔진 버전, 인스턴스 클래스는 `db.t4g.micro`(운영과 다를 수 있음, 운영 RDS 인스턴스 클래스는 확인 안 됨) |
| Redis 버전 | `redis:latest`(측정 시점 기준) — 운영과 동일 이미지 태그 |
| RabbitMQ 버전 | `rabbitmq:3-management`(3.13.7) — 운영과 동일 이미지 태그, 계정도 운영과 동일하게 `admin`/`forday` 2계정 구성 |

## 시드 데이터

`ReactionInitializer`(local 프로파일, 유저 20명·기록 1개짜리 더미)는 부하테스트 규모에 못
쓴다. 대신 `ReactionMeasurementSeeder`(`measure` 프로파일, `@PostConstruct` 아님 —
`CommandLineRunner`로 앱 기동 시 1회 실행)가 시드한다.

| 프로퍼티 | 기본값 | 설명 |
| --- | --- | --- |
| `measure.seed.user-count` | 200 | 게스트 유저 수, `socialId`가 `measure_user_1`..`measure_user_N` 패턴 |
| `measure.seed.record-count` | 1000 | 기록 수, 유저에 라운드로빈으로 분산 배정 |

이미 시드돼 있으면(1번 유저 존재 확인) 재실행하지 않는다 — 4단계 전환 중 재배포가
필요해도 매번 초기화되지 않고 같은 유저·기록 풀을 계속 재사용한다.

k6 `setup()`은 이 패턴화된 `socialId`로 `/auth/guest`를 호출해 시드된 유저로 로그인한다
(신규 유저를 매번 만들지 않음) — [`scripts/k6/common.js`](../../scripts/k6/common.js)의
`setupGuestTokens()` 참고.

부수 효과: 기록 작성자가 유저 풀 전체에 분산돼 있어, 반응자와 작성자가 다른 경우가
대부분이다. 즉 이 부하테스트는 반응 API뿐 아니라 자연스럽게 `NotificationOutboxRelay`
(`#372`)까지 같이 부하를 태운다 — 알림 발행 경로의 실측 데이터도 곁다리로 얻을 수 있다.

## 관측 방법

참고한 실습 자료의 스크린샷(HTTP Request Rate/Duration/Failed)은 k6 **내장 웹 대시보드**다.
실시간 관찰과 사후 정리를 아래처럼 나눈다.

**실시간 — 직접 확인:**
- **k6 웹 대시보드**: `K6_WEB_DASHBOARD=true k6 run ...`로 실행하면 기본 포트 **5665**에
  뜬다. 발생기 EC2 퍼블릭 IP로 `http://<발생기-IP>:5665` 접속 — 보안그룹이 본인 IP로
  제한돼 있다. TPS(HTTP Request Rate)·지연시간(HTTP Request Duration)·실패율(HTTP Request
  Failed)을 실시간으로 보고 여기서 블로그용 스크린샷을 뜬다.
- **AWS 콘솔 CloudWatch**: 측정 대상 EC2의 Monitoring 탭(CPU는 기본 제공, **메모리는
  CloudWatch Agent 필요** — 운영 장애 대응 때와 동일하게 설치돼 있어야 함), RDS 인스턴스의
  Monitoring 탭(CPU·FreeableMemory·IOPS·디스크 큐·스토리지 여유공간 모두 에이전트 없이
  기본 제공)을 열어 병목이 EC2/RDS 중 어디서 나는지 직접 확인한다.

**사후 — 정리:**
- k6 실행 시 항상 `--summary-export=<파일명>.json`을 붙인다. 결과 JSON을
  `docs/perf/results/`에 커밋하면, 단계별 TPS·p50/p95/p99·에러율 비교표·차트로 정리한다.
- CloudWatch 조회 권한이 되면 테스트 시작~종료 시각 기준으로 EC2·RDS의 CPU/메모리/IOPS
  시계열도 같은 정리에 병합한다.

## 3회 반복 · 중위값 규칙

각 단계(①~④)는 3회 반복 실행하고 **TPS는 중위값**을 대표값으로 쓴다. 3회 모두의
`--summary-export` JSON을 `docs/perf/results/<단계>/run-{1,2,3}.json`으로 커밋해 원본을
남기는 게 원칙이다.

> **알려진 미비점(2026-09-17 측정)**: 이번 라운드에서는 JSON을 측정 대상 EC2·발생기 EC2
> 로컬에만 저장하고 레포에 커밋하지 않은 채로 `terraform destroy`를 먼저 실행해버려서,
> 원본 JSON이 유실됐다. 중위값·p95 등 집계된 수치는 `docs/perf/blog-draft.md`와
> `docs/perf/reaction-load-test.md`에 남아 있지만, 재계산 가능한 원본 데이터는 없다.
> 다음 라운드부터는 **`terraform destroy` 실행 전에 `docs/perf/results/`로 scp해 커밋
> 완료를 확인하는 단계를 정리 체크리스트 맨 앞에 둔다.**

## 운영 규모와의 차이 명시

이 환경은 운영과 인스턴스 사양·데이터 규모가 다를 수 있다(위 표 참고). 블로그·PR
어디에 수치를 인용하든 "측정 환경 ≠ 운영 환경"이라는 점과 구체적인 차이를 함께 적는다 —
그렇지 않으면 측정치가 운영 성능인 것처럼 오독된다.

## 측정 중 배포 금지

측정 중 `dev`→`main` 머지가 발생하면 운영 blue-green 배포에는 영향 없지만(측정 환경은
별도 인스턴스), 측정 대상 EC2에 새 이미지를 재배포해야 하는 변경이 섞여 들어가면
측정 결과가 오염된다. 측정 시작 전 진행 중인 PR이 없는지 확인하고, 측정 중에는 이
저장소에 대한 배포성 머지를 보류한다.

## 측정 종료 후 정리 체크리스트

**반드시 이 순서로** — destroy를 먼저 하면 원본 JSON을 다시 못 가져온다(2026-09-17
라운드에서 실제로 이 순서를 어겨 원본을 유실했다).

1. [x] `docs/perf/results/`에 모든 단계의 요약 JSON 커밋 확인 — ⚠️ 2026-09-17 라운드는
   미완료(위 "알려진 미비점" 참고)
2. [x] `docs/perf/blog-draft.md`에 실측치 반영
3. [x] `cd ForDay_Infra && terraform destroy -target=aws_instance.perf_target -target=aws_instance.perf_generator -target=aws_db_instance.perf_db -target=aws_db_subnet_group.perf_db -target=aws_security_group.perf_target_sg -target=aws_security_group.perf_generator_sg -target=aws_security_group.perf_db_sg -target=aws_iam_instance_profile.perf_target -target=aws_iam_role_policy_attachment.perf_target_cloudwatch_agent -target=aws_iam_role_policy_attachment.perf_target_ecr_pull -target=aws_iam_role.perf_target`
4. [x] 이슈 `#374`, `#375` 코멘트로 결과 요약 후 상태 갱신
