# 서버 한 대가 멈춘 날 — k3s 마이그레이션 면접 학습 자료

> 대상 사례: "서버 한 대가 4시간 멈춘 날 — 서비스와 감시 체계를 나누기까지"
> 근거: `ForDay_GitOps`(매니페스트·설계 문서·README), `ForDay_Infra`(Terraform·커밋), `ForDay_Backend`(deploy.yml 이력), GitHub Actions 실행 기록(`gh run view`), **구 EC2 `i-03a8bda067b2b14ae`의 CloudWatch 지표(읽기 전용 조회, 2026-09-30)**
> 표기: `파일:줄` / 커밋 해시 / `CW:` = CloudWatch 조회 결과. 시각은 KST(UTC+9). 괄호 안에 UTC를 같이 적었다.

---

## 1분 요약

> 앱·Redis·RabbitMQ·Prometheus·Grafana를 EC2 한 대에 올려 운영하던 중, 2026-09-16 저녁 CPU가 약 70%로 한 시간 고정된 뒤 인스턴스 상태 검사가 실패하기 시작했고, 다음 날 오전 11시 30분쯤 재부팅할 때까지 **약 15시간** 서비스가 멈춰 있었습니다. 감시하던 Grafana도 같은 서버에 있어서 알림이 오지 않았고, 아침에 사이트 접속이 안 되는 걸 보고서야 알았습니다. 메모리 지표는 없어서 원인 프로세스는 특정하지 못했습니다.
>
> 그래서 두 가지를 나눴습니다. 첫째, **장애 도메인**입니다. k3s 2노드로 앱과 데이터를 다른 노드에 올리고, 워크로드에 메모리 limit과 request를 걸었습니다. 둘째, **감시자**입니다. 감시를 클러스터 밖의 CloudWatch 알람으로 옮겨 SNS → Lambda → Discord로 받게 했습니다. 배포도 SSH 스크립트 대신 Helm + ArgoCD App-of-Apps로 바꿔, 서버가 어떤 상태여야 하는지를 git에 선언해 두었습니다.
>
> 한계도 알고 있습니다. 컨트롤플레인과 데이터 노드는 여전히 각각 단일 장애점이고, 감시는 노드 수준까지만 합니다.

⚠️ 위 요약은 **실제 지표에 맞춰 고친 버전**입니다. 포트폴리오 본문의 "4시간", "CPU 100%"를 그대로 말하면 안 됩니다(0장 1·2번).

---

## 0장. 면접 전에 반드시 알고 갈 것

| # | 무엇이 문제인가 | 증거 | 어떻게 할까 |
|---|---|---|---|
| 1 | **장애 시간 "약 4시간"이 기록과 다르다.** 인스턴스 상태 검사는 19:36~19:46에 처음 실패했고, **20:27부터 다음 날 11:34까지 연속 실패(15시간 7분)** 했다. 알람 추가 커밋에는 "약 14시간 방치"라고 적혀 있다. 설계 문서도 커밋된 버전은 "약 14시간"이고, **작업 폴더에서만 "약 4시간"으로 고쳐 두었다(미커밋)** | `CW: StatusCheckFailed_Instance` 1분 단위, `ForDay_Infra` 84e0fc1 메시지, `ForDay_GitOps` `git diff docs/k8s-migration-design.md` | 본문을 "약 15시간(상태 검사 기준)"으로 고친다. 설계 문서 수정은 되돌리거나 15시간으로 맞춘다 |
| 2 | **"CPU 100% 가까이 고정"이 기록과 다르다.** 5분 평균은 **18:45~19:45에 약 67~70%로 고정**됐고, 최댓값은 21:00 구간의 80.4%였다. 21:20 이후에는 **약 1%** 로 떨어진 채 응답이 없었다. CPU 크레딧 잔고는 576에서 최저 약 512로 줄었을 뿐 **고갈되지 않았다**(초과 크레딧 0). "크레딧이 치솟았다"는 말은 크레딧 **사용량**(5분당 0.1 → 약 7)을 가리킬 때만 맞다 | `CW: CPUUtilization`, `CPUCreditUsage`, `CPUCreditBalance`, `CPUSurplusCreditBalance` | "CPU가 약 70%로 1시간 고정된 뒤 상태 검사가 실패했고, 이후 CPU가 거의 0인 채로 멈춰 있었다"로 고친다 |
| 3 | **CloudWatch 1분 단위 지표는 15일만 보관된다.** 장애는 9/16이라 **1분 단위 상태 검사 기록이 10/1 전후로 사라진다.** 5분 단위는 63일(11월 중순까지), 1시간 단위는 455일 보관된다 | CloudWatch 보관 정책(AWS 공식 문서) | **오늘 바로 콘솔 그래프를 캡처한다**(0장 아래 "캡처 체크리스트") |
| 4 | **k3s 노드의 메모리 알람 2개는 데이터를 한 번도 받지 않았다.** `ForDay/EC2` 네임스페이스의 `mem_used_percent`는 이미 종료된 구 EC2에서만 수집됐다. k3s 두 노드에는 CloudWatch Agent가 없다. `treat_missing_data = "notBreaching"`이라 알람은 늘 OK로 보인다. **실제로 작동하는 알람은 8개가 아니라 6개다** | `CW: list-metrics --namespace ForDay/EC2`, 알람 StateReason "no datapoints were received … treated as [NonBreaching]", `ForDay_Infra/k3s_alarms.tf:79-99` | 두 노드에 Agent를 설치하거나, 본문을 "메모리 알람은 정의만 되어 있다"로 고친다 |
| 5 | **"컨테이너마다 request/limit을 명시해 CPU·메모리 독점을 차단"은 과장이다.** CPU limit은 어떤 워크로드에도 없다. cert-manager·ESO·두 CronJob은 request도 limit도 없다. ArgoCD는 이 저장소가 관리하지 않아 설정을 알 수 없다 | 3부 1번 표 | "주요 워크로드에 메모리 limit과 CPU·메모리 request를 걸었다. CPU는 request 비율로 나눠 쓰게 했다"로 고친다 |
| 6 | **CI 튄 값 4개는 서버 지연이 아니라 재실행(attempt 2)이다.** 411·787·869·9223초 실행은 모두 attempt=2이고, 첫 잡이 시작되기 전 대기가 261~9087초였다. 869초 실행은 build가 끝난 뒤 deploy 잡이 11분 뒤에 다시 돌았다. 787초 실행은 **실패**했다. SSH 배포 잡 자체는 성공 시 37~60초, 실패 시 113·118초였다(헬스체크 루프를 다 돈 경우) | `gh run view <id> --json attempt,jobs` (2부 1번 표) | 본문의 "서버 쪽 지연이 CI 시간에 더해졌다", "나머지 지연 원인은 특정하지 못했다"를 지운다 |
| 7 | **CI 전체 시간은 사실상 줄지 않았다.** 전환 전 1차 시도 성공 21회는 142~195초, 전환 후 4회는 151~193초다. 줄어든 건 배포 잡(37~60초 → 5~18초)이고, 그만큼 빌드가 길어졌다(89~139초 → 137~165초). 그리고 **CI가 끝나도 배포는 아직 끝나지 않았다**(ArgoCD 폴링 + Rollout) | 2부 1번 표 | "CI는 배포 결과를 기다리지 않게 됐다(배포 잡 37~60초 → 5~18초)"로 바꾸고, 전체 시간 비교는 빼거나 "비슷하다"로 쓴다 |
| 8 | **"GitHub Actions Secrets 15개 제거"는 12개다.** 옛 deploy.yml이 참조하던 시크릿은 15개이고, 지금도 3개(FIREBASE_JSON, AWS_ACCESS_KEY_ID/SECRET)를 쓴다. GITOPS_PAT 1개가 새로 생겼다. 쓰지 않는 APPLICATION_PROPERTIES도 남아 있다 | `git show 4822fae^:.github/workflows/deploy.yml`, `gh secret list` | "15개 중 12개를 Secrets Manager·클러스터 설정으로 옮겼다"로 고친다 |
| 9 | **"서버 상태 확인 약 2분 → 10초"는 기록이 없다** | 없음 | 표에서 빼거나 "체감"이라고 밝힌다. 남기려면 오늘 두 방식을 재서 화면 녹화를 해 둔다 |
| 10 | **감시는 노드 수준뿐이다.** HTTP 헬스체크 알람(Route53 헬스체크, Synthetics)이 없다. 앱이 CrashLoop에 빠지거나 인증서가 만료돼도 노드가 살아 있으면 아무 알람도 없다. 배포가 실패해도 알려 줄 곳이 없다(CI는 이미 성공으로 끝났고, ArgoCD 알림도 없다) | `k3s_alarms.tf` 전체, `ForDay_GitOps/apps/` | 면접에서 "감시 체계 분리"를 말할 때 범위(노드)를 먼저 밝힌다 |
| 11 | **설계 문서와 실제 구성이 다른 곳이 세 군데 있다.** ① §8은 데이터 워크로드를 수동 sync로 두기로 했는데, 실제로는 mysql·rabbitmq·redis·mongodb 모두 자동 sync + prune + selfHeal이다. ② §12의 클러스터 안 Prometheus/Grafana는 보류했다. ③ §16의 순차 컷오버 대신 한 번에 전환했다 | `docs/k8s-migration-design.md:75, 98-101, 129`, `apps/mysql.yaml:43-50`, `README.md:56-69` | 면접에서 먼저 "설계와 달라진 점"으로 말한다 |
| 12 | **저장소 상태.** `ForDay_GitOps`: 설계 문서 1줄(14→4시간)이 미커밋이다. 로컬 클론이 원격보다 4커밋 뒤처져 있다(CI 배포 커밋 3개 + PR 머지). `ForDay_Infra`: `perf_test.tf` 미커밋(이 사례와 무관) | `git status`, `gh api repos/For-Day/ForDay_GitOps/commits` | 설계 문서 수정을 정리하고 `git pull` |

### 📸 캡처 체크리스트 (오늘 안에)

1분 단위 데이터가 곧 사라집니다. CloudWatch 콘솔 → 지표 → EC2 → 인스턴스별 지표에서 `i-03a8bda067b2b14ae`(종료된 인스턴스)를 검색하고, 기간은 **2026-09-16 18:00 ~ 09-17 12:30 KST**로 둡니다.

1. `StatusCheckFailed_Instance`: 기간 1분, 통계 Maximum. 19:36, 20:27, 다음 날 11:35가 보이게 찍는다 → **장애 시간 증거**
2. `CPUUtilization`: 기간 5분, 통계 Average와 Maximum 둘 다. 약 70%의 고원과 이후 약 1%로 떨어지는 구간이 보이게 찍는다 → **"100%가 아니었다"는 증거**
3. `CPUCreditBalance`와 `CPUCreditUsage`를 같은 그래프에 넣는다 → **크레딧이 고갈되지 않았다는 증거**
4. `ForDay/EC2 > mem_used_percent`: 첫 데이터가 09-17 11시대(02:00Z)부터 있다 → **메모리 지표가 장애 이후에 생겼다는 증거**
5. CloudWatch 알람 → `forday-k3s-app-memory-high` 상세 화면: StateReason "no datapoints…" → **메모리 알람이 비어 있다는 증거**

CLI로 남기려면(텍스트 증거):
```bash
aws --profile forday-infra --region ap-northeast-2 cloudwatch get-metric-statistics \
  --namespace AWS/EC2 --metric-name StatusCheckFailed_Instance \
  --dimensions Name=InstanceId,Value=i-03a8bda067b2b14ae \
  --start-time 2026-09-16T09:00:00Z --end-time 2026-09-17T03:00:00Z \
  --period 60 --statistics Maximum --output json > status-check-1min.json
```

---

## 파일 색인

| 역할 | 파일 |
|---|---|
| 설계 합의 | `ForDay_GitOps/docs/k8s-migration-design.md` |
| 운영 기록·트러블슈팅 | `ForDay_GitOps/README.md` |
| App-of-Apps 루트 | `ForDay_GitOps/apps/root.yaml` |
| 앱 차트 | `ForDay_GitOps/charts/forday-app/values.yaml`, `templates/rollout.yaml`, `services.yaml`, `ingress.yaml` |
| 데이터 컴포넌트 | `ForDay_GitOps/apps/{mysql,redis,rabbitmq,mongodb}.yaml` |
| 플랫폼 | `ForDay_GitOps/apps/{cert-manager,external-secrets,argo-rollouts,platform}.yaml`, `platform/*.yaml` |
| ECR 인증 | `ForDay_GitOps/workloads/ecr-credentials-sync/{cronjob,rbac}.yaml` |
| DB 백업 | `ForDay_GitOps/workloads/mysql-backup/cronjob.yaml`, `ForDay_Infra/lambda_s3_lifecycle.tf` |
| 노드·IAM·SG | `ForDay_Infra/k3s_cluster.tf`, `terraform.tfvars:29-31` |
| 알람 | `ForDay_Infra/k3s_alarms.tf`, `variables.tf:68-82` |
| 알림 경로 | `ForDay_Infra/lambda_sns.tf`, `lambda_discord_relay.tf`, `discord_relay/index.py` |
| 시크릿 그릇 | `ForDay_Infra/k3s_secrets.tf` |
| 옛/새 배포 | `ForDay_Backend/.github/workflows/deploy.yml` (4822fae 전후) |
| 헬스체크 | `ForDay_Backend/.../TestController.java:40-42`, `application-prod.yml:94-98` |

## 타임라인 (기록으로 확인된 것만)

| 시각 (KST) | 사건 | 근거 |
|---|---|---|
| 09-16 18:40 (09:40Z) | CPU 1% → 47%로 상승 시작 | CW CPUUtilization 5분 |
| 18:45~19:45 | CPU 5분 평균 약 67~70%로 고정 | 같음 |
| 19:36~19:46 | 인스턴스 상태 검사 첫 실패(11분) 후 잠시 회복 | CW StatusCheckFailed_Instance 1분 |
| 19:50~21:15 | CPU 약 8~24%, 21:00 구간에 한 번 80.4% | CW |
| **20:27** | **상태 검사 연속 실패 시작** | CW 1분 |
| 21:20~ | CPU 약 1%, 응답 없음 | CW |
| **09-17 11:35** | **상태 검사 정상화**(재부팅으로 추정) | CW 1분 |
| 11:35~ | `ForDay/EC2 mem_used_percent` 수집 시작(Agent 설치) | CW 1시간 |
| 11:48 | 구 EC2 알람 4종 추가 | `ForDay_Infra` 84e0fc1 |
| 20:20 | 설계 문서 작성 | `ForDay_GitOps` 17e224e |
| 20:35 | k3s 인프라 프로비저닝(Phase 0) | `ForDay_Infra` df845f2 |
| 21:20~22:53 | Phase 1~3 (플랫폼, 데이터, 앱 Rollout) | GitOps 7c7494c~25a8c00 |
| 22:58 | 배포 워크플로 GitOps 전환 | Backend 4822fae |
| **23:14:54** | **컷오버 다운타임 시작**(구 blue 컨테이너 정지) | GitOps README:71 |
| 23:30 | 컷오버 기록 커밋(이 시점에 `/health_check` 200 확인) | GitOps 771062d |
| 09-18 00:00 | 구 EC2 종료, RDS 최종 스냅샷 후 삭제, 구 알람 4종 제거 | Infra 78b006d |

→ **장애 다음 날 하루 저녁(약 4시간) 만에 설계부터 컷오버와 구 인프라 삭제까지 끝냈다.** 면접에서 "그렇게 빨리 한 게 위험하지 않았나?"라는 질문이 나올 수 있다(5부 Q27).

---

# 1부. 기초 개념

### 1-1. 장애 도메인, SPOF, "감시자와 감시 대상이 같이 죽는" 문제
- **정의**: 장애 도메인은 한 원인으로 함께 멈추는 구성 요소의 범위다. SPOF는 그 하나가 멈추면 전체가 멈추는 지점이다.
- **왜 필요한가**: 감시자가 감시 대상과 같은 장애 도메인에 있으면, 가장 알아야 할 장애에서 알림이 오지 않는다.
- **이 프로젝트**: 옛 구성은 앱·Redis·RabbitMQ·Prometheus·Grafana(→ Discord 알림)가 모두 EC2 한 대에 있었다(`design.md:6-12`). 새 구성에서 감시자는 AWS 관리형 CloudWatch(`k3s_alarms.tf`)이고, 알림 경로(SNS·Lambda)도 클러스터 밖에 있다(`lambda_sns.tf:1-9`). 다만 앱 노드(컨트롤플레인+Ingress+앱)와 데이터 노드는 각각 SPOF로 남아 있다(`design.md:32-33, 146-149`).

### 1-2. EC2 기본 지표, CloudWatch Agent, t3 CPU 크레딧
- **정의**: EC2 기본 지표는 하이퍼바이저가 밖에서 볼 수 있는 것만 담는다. CPUUtilization, 네트워크, 디스크 I/O(인스턴스 스토어), StatusCheckFailed_*, t 계열의 크레딧 지표가 여기에 속한다. **메모리와 디스크 사용률은 게스트 OS 안에서만 보이므로** CloudWatch Agent가 커스텀 지표로 보내야 한다.
- **보관**: 1분 단위는 15일, 5분 단위는 63일, 1시간 단위는 455일이다. 기본 모니터링은 5분 간격이지만 상태 검사는 1분 간격이다.
- **t3 크레딧**: t3.small과 t3.medium은 모두 vCPU 2개, 기준 성능 vCPU당 20%, 시간당 24크레딧 적립, 최대 잔고 576이다(1크레딧 = vCPU 1개를 100%로 1분). t3의 기본 모드는 **unlimited**라서 잔고가 0이 되어도 스로틀되지 않고 초과분이 과금된다. standard 모드에서만 기준 성능으로 묶인다. 현재 k3s 두 노드는 모두 `unlimited`다(`describe-instance-credit-specifications`).
- **이 프로젝트**: 장애 당시 잔고 최저 약 512, 초과 크레딧 0이다. 즉 **크레딧 고갈이나 스로틀이 원인이라는 근거는 없다.** 메모리 지표는 장애 뒤 구 EC2에 Agent를 설치하면서 생겼다(84e0fc1). 네임스페이스는 `ForDay/EC2`, 지표 이름은 `mem_used_percent`다(`k3s_alarms.tf:86-87`). k3s 노드에는 Agent가 없다(0장 4번).

### 1-3. cgroup, request와 limit, CPU 스로틀과 OOMKilled, QoS
- **정의**: 컨테이너는 리눅스 cgroup으로 자원을 나눠 쓴다.
  - `requests`는 스케줄러가 노드 여유를 계산할 때 쓰는 **예약값**이자 CPU 경쟁 시 **가중치**(cpu.weight)다.
  - `limits`는 **상한**이다. CPU limit을 넘으면 CFS quota로 **스로틀**(느려짐)되고, 메모리 limit을 넘으면 커널이 **OOMKilled**로 컨테이너를 죽인다.
- **QoS 클래스**:
  - Guaranteed: 모든 컨테이너가 request = limit
  - Burstable: request만 있거나 request < limit
  - BestEffort: 아무것도 없음

  노드 메모리가 부족하면 kubelet은 BestEffort부터 축출한다.
- **왜 필요한가**: 한 프로세스가 노드 메모리를 다 쓰면 OS 전체가 굳는다(이번 장애와 같은 모양으로 추정). limit이 있으면 그 컨테이너만 죽고 재시작된다.
- **이 프로젝트**: 앱은 request cpu 250m/mem 512Mi, limit mem 768Mi → **Burstable**(`values.yaml:10-15`). CPU limit은 어디에도 없다. CPU는 request 비율대로 나눠 쓰고 **남는 CPU는 누구든 가져갈 수 있다.** cert-manager·ESO·CronJob은 **BestEffort**다(3부 1번).

### 1-4. JVM 힙과 컨테이너 limit
- **정의**: `-Xmx`는 힙 상한일 뿐이다. JVM은 힙 밖에서도 메모리를 쓴다(메타스페이스, 스레드 스택(스레드당 약 1MB), 코드 캐시, 다이렉트 버퍼, GC 자료구조 등). 컨테이너 limit은 **이 전부**를 포함한 RSS에 적용된다.
- **왜 필요한가**: `-Xmx`를 limit과 같게 잡으면 힙이 다 차기 전에 OOMKilled가 난다. 반대로 `-Xmx`가 없으면 JVM은 limit의 25%만 힙으로 잡는다(MaxRAMPercentage 기본값).
- **이 프로젝트**: limit 768Mi, `-Xmx512m`으로 힙 밖에 256Mi 여유를 뒀다(`values.yaml:14-20`). `JAVA_TOOL_OPTIONS`로 주입한다(`rollout.yaml:46-47`). 이 비율이 충분한지 **실측한 기록은 없다.** 톰캣 스레드 200개를 다 쓰면 스택만 최대 약 200MB가 될 수 있다.

### 1-5. Kubernetes 기본 오브젝트
- **Pod**: 컨테이너 묶음이자 배치 단위다.
- **Deployment / StatefulSet**: Deployment는 교체 가능한 파드를 관리한다. StatefulSet은 이름과 볼륨이 고정된 파드를 관리한다(MySQL·RabbitMQ·MongoDB, Bitnami 차트가 StatefulSet으로 만든다).
- **Service**: 파드 앞의 고정 가상 IP이자 DNS 이름이다(예: `mysql.forday-data.svc.cluster.local`, `values.yaml:24`).
- **Ingress**: 외부 HTTP(S)를 서비스로 보내는 규칙이다(`ingress.yaml`, Traefik).
- **Namespace**: 이름과 권한의 경계다. `forday-app`, `forday-data`, `monitoring`(비어 있음)을 쓴다(`bootstrap/namespaces.yaml`).
- **노드 라벨·nodeSelector**: `forday.io/role=app|data` 라벨로 워크로드를 노드에 고정한다(`README.md:30-38`). taint는 쓰지 않았다. 그래서 **nodeSelector를 빠뜨린 파드는 어느 노드에든 뜰 수 있다.**

### 1-6. k3s
- **정의**: 쿠버네티스를 단일 바이너리로 묶은 경량 배포판이다. containerd, flannel(CNI), CoreDNS, **Traefik**(Ingress), **ServiceLB**, **local-path-provisioner**를 기본으로 설치한다. 서버가 1대면 etcd 대신 **SQLite**(kine)를 데이터스토어로 쓴다.
- **EKS와 다른 점**: EKS는 컨트롤플레인을 AWS가 관리형 HA로 운영하고(시간당 과금), IRSA와 ECR 자격증명 제공자를 붙여 준다. k3s는 컨트롤플레인을 내가 운영한다. 서버 노드가 곧 컨트롤플레인이라, 그 노드가 죽으면 API도 죽는다.
- **이 프로젝트**: 앱 노드가 k3s server(컨트롤플레인), 데이터 노드가 agent다(`README.md:8-9`). t3.small(2GB)에서는 k3s server와 ArgoCD만으로 가용 메모리가 81MB까지 떨어져 API 서버가 응답하지 않았다. 그래서 t3.medium으로 올렸다(`README.md:91-95`, `terraform.tfvars:29-31`).

### 1-7. liveness·readiness probe와 CI 헬스체크 루프
- **정의**:
  - readiness: 실패하면 서비스 엔드포인트에서 **빠진다**(트래픽 차단).
  - liveness: 실패하면 kubelet이 컨테이너를 **재시작한다**.
  - 기본값: `timeoutSeconds` 1초, `failureThreshold` 3, `periodSeconds` 10초.
- **CI 루프와 다른 점**: CI 루프(`for i in {1..20}; curl; sleep 5`)는 **배포하는 순간 한 번만** 확인한다. probe는 **파드가 살아 있는 동안 계속** 확인한다.
- **이 프로젝트**(`rollout.yaml:57-69`):
  - readiness: `/health_check`, 초기 대기 20초, 5초 간격, 6회 실패 시 unready
  - liveness: `/actuator/health`, 초기 대기 40초, 15초 간격, failureThreshold·timeout은 기본값(3회, 1초) → 약 45초 연속 실패 시 재시작
- ⚠️ **readiness와 liveness의 대상이 뒤집혀 있다.**
  - `/health_check`는 고정 문자열을 반환할 뿐 의존성을 보지 않는다(`TestController.java:40-42`).
  - `/actuator/health`는 Spring Boot가 DB·Redis·MongoDB·RabbitMQ health indicator를 **합쳐서** 보여 준다(Actuator 기본 동작).

  그래서 **데이터 노드가 죽으면 liveness가 실패하고, 앱이 재시작을 반복한다**(3부 2번). 보통은 반대로, liveness는 프로세스 생존만 보고 readiness가 의존성을 본다(`/actuator/health/liveness`, `/readiness` 그룹).

### 1-8. 선언적 배포, GitOps, ArgoCD
- **정의**:
  - 명령형: "이 명령을 실행하라"(SSH + docker run)
  - 선언적: "이 상태여야 한다"(매니페스트)
  - GitOps: git을 원하는 상태의 유일한 원천으로 삼고, 클러스터 안 에이전트(ArgoCD)가 git과 실제 상태를 계속 비교해 맞춘다.
- **ArgoCD 용어**:
  - sync: git 상태를 클러스터에 적용
  - selfHeal: 누가 `kubectl`로 바꿔도 git 상태로 되돌림
  - prune: git에서 지운 리소스를 클러스터에서도 삭제
  - 변경 감지: 기본 **3분 폴링**(webhook 미설정 시, 기본값 — 확인 필요)
- **App-of-Apps**: Application을 만드는 Application이다. 루트 `root.yaml`이 `apps/` 폴더를 보고, 거기 있는 Application 파일(자기 자신 포함)을 만든다(`root.yaml:13-22`). 새 컴포넌트는 파일 하나를 커밋하면 된다(예: MongoDB 4b58408).
- **이 프로젝트**: 모든 Application이 `automated: {prune: true, selfHeal: true}`다. 설계 문서 §8(데이터는 수동 sync)과 다르다.

### 1-9. Helm 차트와 values
- **정의**: 템플릿(`templates/*.yaml`)에 값(`values.yaml`)을 채워 매니페스트를 만드는 패키징 도구다.
- **이 프로젝트**:
  - 앱은 자체 차트다(`charts/forday-app`). CI는 `values.yaml`의 `image.tag` 한 줄만 바꾼다(new `deploy.yml:107-110`).
  - 데이터·플랫폼은 커뮤니티 차트에 `helm.values`를 덮어쓴다(`apps/mysql.yaml:10-40`).
  - Bitnami 무료 이미지가 `bitnamilegacy/*`로 옮겨가서 `allowInsecureImages: true`가 필요했다(`README.md:101-104`).

### 1-10. Argo Rollouts blue/green
- **정의**: active 서비스와 preview 서비스를 둔다.
  - 새 버전(green)을 preview에 먼저 띄우고, 준비되면 active의 selector를 새 ReplicaSet으로 바꿔 한 번에 전환한다.
  - `autoPromotionEnabled: true`면 새 ReplicaSet이 Available이 되는 즉시 전환한다.
  - `scaleDownDelaySeconds`: 전환 뒤 옛 파드를 남겨 두는 시간
- **이 프로젝트**: `autoPromotionEnabled: true`, `scaleDownDelaySeconds: 30`(`values.yaml:44-48`, `rollout.yaml:70-75`).
  - **AnalysisTemplate(에러율 기반 자동 판단)은 없다.** 전환 조건은 readiness뿐이다.
  - 배포 중에는 앱 파드가 2개(request 512Mi × 2)가 된다(`design.md:87`).

### 1-11. 이미지 pull 인증: ECR 토큰
- **정의**: ECR `get-login-password` 토큰은 **12시간** 유효하다. 파드는 `imagePullSecrets`로 docker-registry 타입 Secret을 참조해 인증한다.
- **k3s에서 노드 IAM 역할만으로 안 되는 이유**: EKS AMI에는 kubelet용 ECR credential provider 플러그인이 설정돼 있다. 이 플러그인이 노드 IAM 역할로 토큰을 자동으로 받는다. k3s에는 그 설정이 기본으로 없다. (k3s도 kubelet `--image-credential-provider-config`에 `ecr-credential-provider` 바이너리를 붙이면 된다 — 대안, 4부 7번. 지원 버전은 확인 필요)
- **이 프로젝트**: CronJob이 6시간마다(`"0 */6 * * *"`) aws-cli로 토큰을 받는다. 그다음 `kubectl create secret … --dry-run=client -o yaml | kubectl apply`로 `ecr-pull-secret`을 갱신한다(`cronjob.yaml:8, 28-38`). 파드는 이 Secret을 참조한다(`rollout.yaml:20-21`).

### 1-12. RBAC: ServiceAccount, Role, RoleBinding
- **정의**: ServiceAccount는 파드의 신원이다. Role은 한 네임스페이스 안에서 허용되는 동사와 리소스의 목록이고, RoleBinding이 둘을 잇는다.
- **이 프로젝트**(`rbac.yaml:11-19`): `forday-app` 네임스페이스의 `secrets`에 `get, create, update, patch`를 허용한다.
  - 클러스터 전체가 아니라 네임스페이스로 좁히고 `delete`·`list`를 뺀 것은 최소 권한 쪽이다.
  - ⚠️ 하지만 `resourceNames`로 `ecr-pull-secret`만 지정하지 않았다. 그래서 같은 네임스페이스의 **`forday-app-secrets`(DB 비밀번호·JWT·OpenAI 키 등)까지 읽을 수 있다.** `create`는 `resourceNames`로 제한할 수 없다. 그래서 흔히 Secret을 미리 만들어 두고 `get/update/patch`만 이름으로 제한한다.
  - AWS 권한은 ServiceAccount가 아니라 **노드 IAM 역할 전체**에서 온다(IMDS). 이 파드는 노드 역할이 가진 Secrets Manager 읽기와 S3 쓰기 권한도 쓸 수 있다(`k3s_cluster.tf:99-140`).

### 1-13. 시크릿 관리: Secrets Manager + External Secrets Operator
- **정의**:
  - ESO는 `ClusterSecretStore`(어디서 가져올지)와 `ExternalSecret`(무엇을 어떤 k8s Secret으로 만들지)을 읽는다.
  - 그리고 `refreshInterval`마다 AWS Secrets Manager 값을 k8s Secret으로 동기화한다.
  - git에는 참조만 남고 값은 남지 않는다.
- **이 프로젝트**:
  - 스토어는 인증 설정 없이 노드 인스턴스 프로파일을 쓴다(`cluster-secret-store.yaml:1-13`).
  - 앱 시크릿 12개 키를 `forday-app-secrets` 하나로 모은다(`forday-app-external-secret.yaml:18-66`). `refreshInterval: 1h`다.
  - Terraform은 시크릿 "그릇"만 만든다. 값은 state에 남지 않도록 CLI로 따로 넣었다(`k3s_secrets.tf:3-11`). 그릇은 처음 6개였고, MongoDB가 추가돼 7개가 됐다(c4ef488).
- ⚠️ **일원화가 덜 된 곳이 있다.**
  - `FIREBASE_JSON`은 여전히 GitHub Secret이다. CI가 이 값을 `src/main/resources`에 파일로 만들고, 그 파일이 jar와 이미지에 **구워진다**(new `deploy.yml:38-43`). `Dockerfile`의 "이미지에는 시크릿을 굽지 않는다" 주석과 모순된다.
  - `AWS_ACCESS_KEY/SECRET_KEY`(S3)도 아직 정적 키다(`k3s_secrets.tf:22`).

### 1-14. 상태 저장 워크로드: PV/PVC, local-path, mysqldump, S3 수명주기
- **정의**:
  - PVC는 볼륨 요청이고, PV는 실제 볼륨이다.
  - local-path는 노드 로컬 디스크 디렉터리를 PV로 쓴다. **PV가 그 노드에 묶여서**, 노드가 죽으면 다른 노드로 옮길 수 없다.
  - mysqldump는 논리 백업이다. `--single-transaction`이면 InnoDB를 잠그지 않고 일관된 스냅샷을 뜬다.
- **이 프로젝트**:
  - MySQL 10Gi(`apps/mysql.yaml:31-34`), RabbitMQ 4Gi, MongoDB 5Gi를 데이터 노드 로컬 디스크(40GB, `variables.tf:217-221`)에 둔다.
  - 백업은 **매일 03:00 KST**에 돈다(`mysql-backup/cronjob.yaml:13-14`). `mysqldump | gzip` → emptyDir → aws-cli가 `s3://forday-s3-bucket/db-backups/mysql/`에 올린다.
  - 30일 뒤 만료된다(`lambda_s3_lifecycle.tf:14-25`). 노드 역할은 해당 경로에 `PutObject`만 할 수 있다(`k3s_cluster.tf:128-140`).
  - **MongoDB 백업은 없다.** 복원 리허설 기록도 없다.

### 1-15. 알림 경로: CloudWatch 알람 → SNS → Lambda → Discord
- **정의**: 알람은 상태가 바뀔 때(OK↔ALARM) SNS에 게시한다. SNS는 구독한 Lambda를 비동기로 호출한다. Lambda는 알람 JSON을 Discord 임베드로 바꿔 웹훅에 POST한다.
- **왜 중계 Lambda가 필요한가**: SNS가 Discord 웹훅 형식으로 직접 보낼 수 없다(`index.py:1-7`).
- **이 프로젝트**:
  - 이미지 리사이즈 Lambda 알람과 **같은 토픽**(`forday-lambda-alarms`)과 같은 채널을 쓴다(`k3s_alarms.tf:31-32`).
  - `ok_actions`도 걸려 있어 복구 알림도 온다.
  - Discord(Cloudflare)가 기본 urllib User-Agent를 403으로 막아서 UA를 명시했다(`index.py:65-67`).

---

# 2부. 흐름 따라가기

## (1) 전환 전 배포: SSH blue/green (`git show 4822fae^:.github/workflows/deploy.yml`)

| 단계 | 누가 | 무엇을 | 줄 |
|---|---|---|---|
| 1 | GitHub Actions `build` 잡 | 테스트 → bootJar → 이미지 빌드 → ECR `:latest` 푸시 | 48-80 |
| 2 | `deploy` 잡 | SSH 키와 EC2 IP(시크릿)로 접속 | 86-97 |
| 3 | EC2 셸 | nginx `service-env.inc`로 현재 색을 읽고 반대 색(포트 8080/8081)을 정함 | 106-124 |
| 4 | EC2 셸 | `docker pull :latest` → 대상 컨테이너 stop/rm → `docker run -e …`(시크릿 11개와 평문 RabbitMQ 비밀번호 `forday1234` 주입) | 125-153 |
| 5 | EC2 셸 | `/health_check`를 5초 간격 최대 20회 호출 | 155-170 |
| 6 | EC2 셸 | nginx 설정 교체 → `nginx -s reload` → 옛 컨테이너 제거 → `docker image prune -af` | 172-179 |

- **CI가 기다리는 범위**: 새 컨테이너가 뜨고 헬스체크를 통과해 트래픽 전환이 끝날 때까지다. 실측한 배포 잡 시간은 아래 표에 있다.
- **실패하면**: 헬스체크 20회 실패 → `docker logs` 출력 → `exit 1`. 이때 nginx는 옛 색 그대로이고 새 컨테이너는 떠 있는 채로 남는다. CI가 빨갛게 표시돼 사람이 알 수 있다.
- **상태의 원천**: 서버에 무엇이 떠 있는지는 "마지막으로 실행한 스크립트의 결과"다. `:latest` 태그라 어떤 커밋이 떠 있는지 이미지 태그로는 알 수 없다.

### CI 실행 시간 재계산 (`gh run view --json attempt,createdAt,updatedAt,jobs`)

| 구분 | 표본 | 총 시간(생성→종료) | build 잡 | 배포 잡 | 비고 |
|---|---|---|---|---|---|
| 전환 전, 1차 시도 성공 | 21회 (04-02 ~ 09-17) | **142~195초** | 89~139초 | **37~60초** | |
| 전환 전, 1차 시도 실패 | 1회 (ccc7c1b) | 227초 | 106초 | **113초** | 헬스체크 루프 소진 |
| 전환 전, **재실행(attempt 2)** | 4회 | **411 / 787(실패) / 869 / 9223초** | 89~136초 | 44 / **118(실패)** / 60 / 44초 | 첫 잡 시작 전 대기 261 / 568 / 3 / **9087초**. 869초는 build 뒤 deploy 재실행까지 11분 공백 |
| 전환 후 | 4회 (09-17 ~ 09-20) | **151~193초** | 137~165초 | **5~18초** (update-gitops) | |

- 포트폴리오의 "25회"는 0d98f0a(04-02)부터 23d2399(09-17)까지 전환 직전 25회와 개수가 맞는다. 그런데 이 25회 안에 **9223초와 411초도 들어 있고**, 최솟값은 145가 아니라 **142초**다.
- 튄 값은 전부 **재실행이라 첫 시도부터 재실행까지의 벽시계 시간이 포함된 것**이다. 서버 쪽 지연과는 무관하다.
- 배포 잡이 실패할 때 113~118초가 걸린 것은 `20회 × 5초 = 100초` 루프에 SSH·pull 시간이 더해진 값과 맞는다. "최대 100초 대기"는 **실측으로 뒷받침된다.**

## (2) 전환 후 배포: GitOps (`deploy.yml` @4822fae, `ForDay_GitOps`)

| 단계 | 누가 | 무엇을 | 근거 |
|---|---|---|---|
| 1 | Actions `build` | 테스트 → 이미지 → 태그 = 커밋 SHA 앞 12자리, `:<sha>`와 `:latest` 푸시 | `deploy.yml:82-93` |
| 2 | Actions `update-gitops` | `GITOPS_PAT`로 GitOps 저장소 체크아웃 → `sed`로 `image.tag` 교체 → 커밋·푸시(`forday-ci`) | `deploy.yml:95-122` |
| — | **CI 종료** | 여기서 끝난다. 배포 결과를 모른다 | |
| 3 | ArgoCD | `forday-app` Application이 git 변경을 감지한다(기본 3분 폴링, 기본값 — 확인 필요) → Helm 렌더 → Rollout 스펙 변경 | `apps/forday-app.yaml` |
| 4 | Argo Rollouts | 새 ReplicaSet(green) 생성, preview 서비스에 연결 | `rollout.yaml:70-75` |
| 5 | kubelet | `ecr-pull-secret`으로 이미지 pull → 컨테이너 시작 | `rollout.yaml:20-24` |
| 6 | kubelet | readiness `/health_check` 통과(20초 뒤부터 5초 간격) | `rollout.yaml:57-63` |
| 7 | Argo Rollouts | 새 RS가 Available → `autoPromotionEnabled` → active 서비스 selector를 새 RS로 교체(트래픽 전환) | `values.yaml:47` |
| 8 | Argo Rollouts | 30초 뒤 옛 RS 축소 | `values.yaml:48` |
| 9 | Traefik | Ingress `forday.kr` → `forday-app-active` | `ingress.yaml:15-24` |

- **CI가 기다리는 범위**: GitOps 커밋 푸시까지다. 이미지 pull, 기동, probe, 전환은 모두 CI 밖에서 일어난다.
- **실패하면**:
  - 이미지 pull 실패나 readiness 실패 → 새 RS가 준비되지 않음 → **active는 옛 버전 그대로다**(자동 전환이 안 일어남).
  - Rollout은 진행 기한(기본 600초, 기본값 — 확인 필요)이 지나면 Degraded가 된다.
  - **알려 주는 곳이 없다.** CI는 초록이고, ArgoCD Notifications도 없고, CloudWatch 알람은 노드만 본다. ArgoCD UI를 직접 열어야 안다.
- **상태의 원천**: `values.yaml`의 `image.tag`가 곧 운영 버전이다. `ForDay_Server@<sha>`가 커밋 메시지에 남는다(원격 e9c1aae 등).

## (3) 장애 감지 (`ForDay_Infra/k3s_alarms.tf`)

| 알람 (노드마다) | 지표 | 조건 | 발화까지 |
|---|---|---|---|
| status-check-failed-instance | `AWS/EC2 StatusCheckFailed_Instance` | 60초 Maximum ≥ 1, 2회 | 약 2분 |
| status-check-failed-system | `AWS/EC2 StatusCheckFailed_System` | 같음 | 약 2분 |
| cpu-high | `AWS/EC2 CPUUtilization` | 300초 평균 ≥ 85%, 3회 | 약 15분 |
| memory-high | `ForDay/EC2 mem_used_percent` | 300초 평균 ≥ 85%, 2회 | 약 10분 — **데이터 없음, 절대 울리지 않음** |

- **누가**: CloudWatch(관리형) → SNS `forday-lambda-alarms` → Lambda `forday-cloudwatch-discord-relay` → Discord 웹훅
- **실패하면**:
  - `treat_missing_data = "notBreaching"`이다. 그래서 **인스턴스가 stop/terminate되어 지표가 끊기면 상태 검사 알람도 울리지 않는다.** OS가 굳은 경우(이번 장애)는 상태 검사가 1을 보고하므로 잡힌다.
  - Discord 웹훅이 실패하면 Lambda 오류가 난다. SNS는 Lambda를 비동기로 호출하므로 Lambda 쪽 재시도(기본 2회)가 있다. **중계 Lambda 자체의 실패를 알리는 알람은 없다.**

### ✅ 이번 장애 지표를 새 알람에 대입해 보면
- 상태 검사 알람: 19:36부터 1이었으므로 **19:38쯤 Discord 알림이 왔을 것이다**(2분 평가).
- CPU 알람: 5분 평균 최대 약 70%라 **85% 임계치를 넘지 못한다. 울리지 않았을 것이다.**
- 메모리 알람: 구 EC2였다면 Agent가 있어 유효했을 것이다. 지금 k3s 노드는 데이터가 없다.

→ 이 대입은 면접에서 "알람이 실제로 그 장애를 잡았을까?"에 대한 **근거 있는 답**이 된다. 다만 알람 → Discord 경로는 SNS 테스트 발행으로만 검증했다(84e0fc1, Infra `README.md:120-122`). `aws cloudwatch set-alarm-state`로 알람 상태를 바꿔 끝까지 흘려 본 기록은 없다.

## 시퀀스 다이어그램

```
[(2) GitOps 배포]
Dev      GitHub Actions        ECR     ForDay_GitOps     ArgoCD        Argo Rollouts      kubelet(app node)   Traefik
 | push main |                  |           |               |                 |                  |              |
 |---------->| test/build        |           |               |                 |                  |              |
 |           |--push :<sha>----->|           |               |                 |                  |              |
 |           |--commit tag=<sha>------------>|               |                 |                  |              |
 |           |  (CI 종료, 초록)   |           |<--poll(~3m)---|                 |                  |              |
 |           |                  |           |               |--apply Rollout->|                  |              |
 |           |                  |           |               |                 |--new RS(green)-->|              |
 |           |                  |<------------------- pull (ecr-pull-secret) ------------------|              |
 |           |                  |           |               |                 |<--readiness OK---|              |
 |           |                  |           |               |                 |--active selector→green ------->|
 |           |                  |           |               |                 |--(30s) scale down blue          |
 |           |                  |           |               |    실패 시: active=blue 유지, 알림 없음(UI에서만 확인)    |

[(3) 장애 감지]
EC2 node      CloudWatch(AWS)                  SNS topic           discord_relay Lambda      Discord
  | OS hang      |                               |                      |                      |
  |--Status=1--->| 60s×2 ≥1 → ALARM              |                      |                      |
  |              |--publish(AlarmName,State)---->|                      |                      |
  |              |                               |--invoke(async)------>|                      |
  |              |                               |                      |--POST embed(red)---->|
  | recover      |--OK------------------------->|--------------------->|--POST embed(green)-->|
  (인스턴스 stop/terminate → 지표 없음 → notBreaching → 알람 없음)
```

---

# 3부. 고려한 상황과 경계 사례

### 1. 메모리 limit을 넘으면? CPU limit은? — 전체 워크로드 자원 표

| 워크로드 | 노드 | CPU req | CPU limit | Mem req | Mem limit | QoS | 근거 |
|---|---|---|---|---|---|---|---|
| forday-app (Rollout) | app | 250m | **없음** | 512Mi | 768Mi | Burstable | `values.yaml:10-15` |
| MySQL | data | 100m | 없음 | 512Mi | 1Gi | Burstable | `apps/mysql.yaml:35-40` |
| RabbitMQ | data | 100m | 없음 | 256Mi | 512Mi | Burstable | `apps/rabbitmq.yaml:31-36` |
| Redis | data | 50m | 없음 | 128Mi | 256Mi | Burstable | `apps/redis.yaml:32-37` |
| MongoDB | data | 100m | 없음 | 256Mi | 512Mi | Burstable | `apps/mongodb.yaml:50-55` |
| Argo Rollouts 컨트롤러 | app | 50m | 없음 | 128Mi | 256Mi | Burstable | `apps/argo-rollouts.yaml:19-24` |
| cert-manager (3종) | app | — | — | — | — | **BestEffort** | `apps/cert-manager.yaml:15-27` |
| External Secrets (3종) | app | — | — | — | — | **BestEffort** | `apps/external-secrets.yaml:18-27` |
| ecr-credentials-sync CronJob | app | — | — | — | — | **BestEffort** | `cronjob.yaml:21-41` |
| mysql-backup CronJob | data | — | — | — | — | **BestEffort** | `mysql-backup/cronjob.yaml:29-64` |
| ArgoCD, Traefik, CoreDNS 등 | app | 알 수 없음 | | | | | 이 저장소가 관리하지 않음(ArgoCD는 수동 설치·`kubectl patch`, `README.md:88-90`) |
| k3s server 프로세스 자체 | app | cgroup 밖(systemd) | | | | | k3s 기본 설정. `system-reserved` 미설정(확인 필요) |

- **메모리 limit 초과**: 해당 컨테이너만 OOMKilled되고 재시작된다(Rollout·StatefulSet이 다시 띄움). 노드 전체가 굳는 것은 막는다. 단 **limit 합계가 노드 메모리보다 크면**(overcommit), 모두가 동시에 limit 가까이 쓸 때 노드 수준 메모리 압박이 다시 올 수 있다.
  - 앱 노드(4GiB): 앱 768Mi × **2**(blue/green 전환 중) + Rollouts 256Mi + BestEffort 다수 + k3s server + ArgoCD
  - Phase 3 중 여유 메모리를 741MB로 기록했다(`README.md:59-61`). 전환 중 파드 1개가 더 뜨면 여유가 거의 없다.
- **CPU**: limit이 없으므로 **한 컨테이너가 노드 CPU를 100% 쓸 수 있다.** 다른 컨테이너와 경쟁할 때만 request 비율(250:50:…)로 나뉜다. BestEffort 파드는 최소 가중치를 받는다. k3s server는 파드 cgroup 밖이라 보호받지 못한다.
- **현재 구성의 한계**: "CPU 독점 차단"은 성립하지 않는다. 메모리도 BestEffort 파드와 k3s 자신은 보호하지 못한다.
- **개선하려면**:
  - 모든 파드에 request를 건다. 필요하면 LimitRange로 네임스페이스 기본값을 둔다.
  - k3s `--kube-reserved/--system-reserved`로 시스템 몫을 예약한다.
  - CPU limit은 스로틀 부작용(JVM GC 지연)을 감안해 앱에만 선택적으로 건다.

### 2. 앱 노드가 죽으면? 데이터 노드가 죽으면?
- **앱 노드**: 컨트롤플레인, Traefik, 앱, ArgoCD, cert-manager, ESO가 모두 여기 있다. 그래서 **서비스 전체가 중단된다.** DNS가 이 노드의 EIP 하나만 가리킨다(`k3s_cluster.tf:197-205`).
  - 알람: OS가 굳으면 status-check-instance가 2분 뒤, AWS 하드웨어 문제면 status-check-system이 울린다.
  - 인스턴스가 stop되면 **알람이 울리지 않는다**(notBreaching).
- **데이터 노드**: MySQL·Redis·RabbitMQ·MongoDB가 모두 멈춘다.
  - 앱 파드는 살아 있지만 `/actuator/health`가 DOWN(503)이 된다. 그래서 **liveness가 45초 뒤 실패해 앱이 재시작을 반복한다**(1-7). readiness는 `/health_check`라 통과한다. 재시작 사이사이 앱이 트래픽을 받으면 DB 오류를 낸다.
  - 쿠버네티스는 노드를 NotReady로 표시한다. 약 5분 뒤 파드를 축출 대상으로 표시하지만(기본 toleration 300초, 기본값), nodeSelector와 local-path PV 때문에 **옮길 곳이 없다.**
  - 알람: 데이터 노드 status-check 알람만 울린다.
- **현재 구성의 한계**: 두 노드 모두 SPOF다. 게다가 데이터 노드 장애가 앱 재시작 폭주로 번진다.
- **개선하려면**: liveness를 `/actuator/health/liveness`로, readiness를 `/actuator/health/readiness`(DB 포함)로 바꾼다. 데이터는 EBS 스냅샷이나 복제로 보호한다(§17).

### 3. 컨트롤플레인(앱 노드)만 죽으면 데이터 노드의 파드는?
- 데이터 노드의 kubelet과 containerd는 이미 떠 있는 컨테이너를 **계속 실행한다.** API 서버가 없으므로 새 스케줄링, 재시작 정책 변경, Secret 갱신, CronJob 실행은 멈춘다. 백업 CronJob도 돌지 않는다.
- 다만 이 구성에서는 Ingress와 앱도 같은 노드에 있어서, 데이터 노드가 살아 있어도 서비스는 중단이다.
- **한계**: 컨트롤플레인 HA가 없다.
- **개선하려면**: 서버 3대 임베디드 etcd HA(§17). 비용이 핵심 제약이라 보류했다.

### 4. 클러스터 전체가 죽으면 누가 알려 주나? 클러스터 안 모니터링은?
- CloudWatch 알람은 AWS 쪽에서 평가하므로 클러스터와 무관하게 동작한다. **노드 OS가 굳거나 하드웨어가 고장 나면** 알린다.
- **클러스터 안 모니터링은 배포되지 않았다.** `monitoring` 네임스페이스는 비어 있다(`bootstrap/namespaces.yaml:11-14`, `README.md:56-63`).
- 노드는 멀쩡한데 **앱이나 k3s만 죽은 경우**(CrashLoop, API 서버 OOM, Traefik 장애, 인증서 만료, DB 파드 OOM 반복)는 **아무도 알려 주지 않는다.**
- **한계**: 노드 수준 감시만 있다. 사용자가 보는 증상(HTTP 응답)을 보지 않는다.
- **개선하려면**: 비용이 거의 들지 않는 것부터 한다.
  - Route53 헬스체크(`https://forday.kr/health_check`) + CloudWatch 알람 → 같은 SNS
  - k3s 노드에 CloudWatch Agent를 설치해 메모리와 디스크를 수집
  - ArgoCD Notifications로 sync·health 실패를 Discord로 보냄

### 5. ECR 토큰 CronJob이 실패하면?
- 토큰은 12시간 유효하고 갱신은 6시간마다다. **한 번 실패해도** 이전 토큰이 최대 6시간 더 유효하다. **두 번 연속 실패하면**(최대 약 12시간 뒤) Secret 안 토큰이 만료된다.
- 그래도 **이미 떠 있는 파드는 영향이 없다.** 앱 태그는 SHA라 `imagePullPolicy` 기본값이 `IfNotPresent`이고, 노드 캐시에 이미지가 있으면 pull하지 않는다.
- 깨지는 순간은 **새 이미지 배포**(새 SHA)다. 그때 `ImagePullBackOff`가 나고, Rollout은 전환하지 않아 옛 버전이 계속 서비스한다. **CI는 초록이고 알림도 없다.**
- CronJob 자체도 약점이 있다.
  - 매 실행마다 인터넷에서 kubectl을 **내려받는다**(`cronjob.yaml:29-31`). dl.k8s.io 장애나 egress 문제에 약하다.
  - Secret이 처음 만들어지기 전(설치 직후 최대 6시간)에는 pull할 수 없다. 최초 Secret을 어떻게 만들었는지 기록이 없다(수동 실행 추정).
- **개선하려면**:
  - kubectl이 들어 있는 이미지를 쓴다.
  - 실패한 Job을 감시한다.
  - 근본적으로는 kubelet credential provider(`ecr-credential-provider`)로 바꿔 Secret 자체를 없앤다.

### 6. ArgoCD sync가 실패하거나 GitOps 토큰이 만료되면?
- ArgoCD가 저장소를 읽는 토큰은 **담당자의 `gh` CLI 토큰을 임시로 쓰는 중이다.** 만료되면 "sync가 조용히 멈춘다"(`README.md:48-54`). 이미 떠 있는 버전은 계속 돈다. 새 배포만 반영되지 않는다.
- CI가 쓰는 `GITOPS_PAT`가 만료되면 `update-gitops` 잡이 실패한다. **이건 CI가 빨갛게 표시돼 알 수 있다.**
- CRD 캐시 문제로 `platform` 앱이 재시도 루프에 갇힌 적이 있다. hard-refresh로 안 돼서 Application을 삭제하고 다시 만들어 풀었다(`README.md:98-100`).
- **한계**: 읽기 토큰 만료를 알 방법이 없다.
- **개선하려면**: 저장소 전용 fine-grained PAT나 GitHub App, 또는 deploy key를 쓰고, ArgoCD Notifications를 켠다.

### 7. 잘못된 이미지 태그가 커밋되면? 자동 롤백은?
- 존재하지 않는 태그: pull 실패 → 새 RS가 준비되지 않음 → active는 옛 RS 그대로 → **사용자 영향은 없다.** Rollout은 Degraded 상태로 멈춘다.
- 이미지는 뜨는데 **로직이 깨진 경우**: `/health_check`는 늘 200이다. 그래서 readiness를 통과해 **자동 전환된다.** AnalysisTemplate이 없어 에러율로 멈추지 않는다.
  - 30초(`scaleDownDelaySeconds`) 안에는 `kubectl argo rollouts undo`나 abort로 옛 RS를 즉시 되살릴 수 있다.
  - 그 뒤라면 git revert(이미지 태그 되돌리기) → ArgoCD sync → 새 blue/green 과정을 거친다.
- **자동 롤백 조건은 없다.**
- **개선하려면**: readiness에 의존성을 포함한다. `prePromotionAnalysis`로 preview 서비스에 스모크 테스트를 돌리고, `autoPromotionEnabled: false` + 수동 승인도 검토한다.

### 8. Secrets Manager 값이 바뀌면 파드에 언제 반영되나?
- ESO가 최대 1시간(`refreshInterval: 1h`) 안에 k8s Secret을 갱신한다.
- 하지만 앱은 Secret을 **환경변수(`secretKeyRef`)** 로 읽는다(`rollout.yaml:48-54`). 환경변수는 컨테이너가 시작될 때 고정되므로 **파드가 재시작되기 전까지 반영되지 않는다.**
- MySQL·RabbitMQ 비밀번호는 차트가 **최초 초기화 때만** 계정을 만든다(Bitnami 동작, 확인 필요). Secret만 바꾸면 DB 안의 실제 비밀번호와 어긋나 앱 접속이 깨질 수 있다.
- **개선하려면**: Reloader 같은 도구로 Secret 변경 시 롤아웃한다. DB 비밀번호 교체 절차(DB 쪽 ALTER USER → Secret 교체 → 재시작)를 문서화한다.

### 9. MySQL 파드나 볼륨이 날아가면?
- **파드만 죽으면**: StatefulSet이 같은 노드에 다시 띄운다. PV는 로컬 디스크에 남아 있으니 데이터도 유지된다.
- **PVC를 지우거나 노드 디스크를 잃으면**: 마지막 백업(매일 03:00 KST)으로 복원한다. **최대 약 24시간 치 쓰기를 잃는다**(RPO ≤ 24h). RDS는 PITR이 기본 5분 단위였다(기본값, 확인 필요).
- 복원 절차는 문서가 없고 **리허설한 적도 없다.** 노드 역할에는 `db-backups/*`에 대한 `PutObject`만 있어서 복원에 필요한 `GetObject` 권한이 따로 필요하다(`k3s_cluster.tf:128-140`).
- 컷오버 때 복원하면서 `ERROR 3780`(FK collation 불일치)을 겪었다. Hibernate가 미리 만든 빈 스키마를 `DROP DATABASE` → `CREATE DATABASE`로 지운 뒤 복원해 풀었다(`README.md:120-124`). → 복원할 때는 **빈 DB에서 시작해야 한다**는 교훈이다.
- 백업 Job이 실패해도 알림이 없다. `failedJobsHistoryLimit: 3`으로 흔적만 남는다.
- **prune 위험**: MySQL Application이 `prune: true`다(`apps/mysql.yaml:43-50`). 설계 §8은 이 위험 때문에 수동 sync를 권했다. 실무에서는 StatefulSet의 volumeClaimTemplate으로 생긴 PVC는 ArgoCD 관리 대상이 아니라 prune으로 지워지지 않는 경우가 많다. 이 점도 확인이 필요하다.
- **개선하려면**: 복원 리허설을 하고 소요 시간을 기록한다. 백업 Job 실패 알림을 붙이고, 데이터 노드 EBS 스냅샷(DLM)을 추가하고, MongoDB 백업도 추가한다.

### 10. 메모리 알람은 두 노드에서 실제로 수집되나?
- **안 된다.** `list-metrics --namespace ForDay/EC2` 결과 데이터가 있는 인스턴스는 구 EC2 `i-03a8bda067b2b14ae` 하나다.
- 두 노드 알람의 StateReason은 "no datapoints were received for 2 periods … treated as [NonBreaching]"이다.
- Terraform에는 Agent 설치가 없다(user_data 없음, `k3s_cluster.tf:149-191`). IAM 정책(`CloudWatchAgentServerPolicy`)만 붙어 있다(`:99-102`).
- **한계**: 장애의 원인으로 가장 의심되는 메모리를 지금도 보지 못한다.
- **개선하려면**: user_data나 SSM으로 Agent를 설치하고 설정을 Terraform으로 관리한다. 메모리 알람은 `treat_missing_data = "breaching"`이나 `missing`으로 바꿔 수집이 끊기는 것 자체를 알린다.

### 11. 마이그레이션 중 실제로 겪은 문제 (`GitOps README.md:83-128`, `Infra README.md:145-157`)

| 문제 | 해결 | 교훈 |
|---|---|---|
| t3.small 앱 노드가 k3s + ArgoCD만으로 가용 메모리 81MB, API 서버 응답 불가 | t3.medium으로 증설, EIP 덕에 IP 유지 | 컨트롤플레인도 메모리를 쓴다. 사이징은 실측 뒤에 |
| Traefik·ArgoCD 파드가 데이터 노드에 뜸 | HelmChartConfig와 `kubectl patch`로 nodeSelector | taint 없이 라벨만 쓰면 빠뜨린 파드가 새어 나간다. ArgoCD 패치는 git에 없다(드리프트) |
| `bitnami/*` 이미지 삭제 | `bitnamilegacy/*` + `allowInsecureImages` | 외부 이미지 공급망 변화에 취약하다. 장기적으로 대체 이미지 필요 |
| ESO 0.10.7 CRD가 v1 미지원 | v1beta1로 수정 | 차트 버전과 API 버전을 맞춘다 |
| ArgoCD CRD 캐시로 재시도 루프 | Application 삭제 후 재생성 | hard-refresh로 안 되는 경우도 있다 |
| Application의 helm.values 변경이 반영 안 됨 | 루트 앱을 hard-refresh | App-of-Apps에서는 부모가 자식 스펙을 소유한다 |
| k3s 인증서 SAN에 공인 IP 없음 | `tls-san` 추가 | |
| 관리자 IP 변경 → SSH·kubectl 동시 타임아웃(장애처럼 보임) | `k3s_admin_cidr` 갱신 | 증상이 비슷해도 원인은 다를 수 있다 |
| mysqldump PROCESS 권한 | root로 1회 GRANT | 차트 values로는 표현할 수 없다 → PVC를 새로 만들면 다시 해야 한다 |
| 덤프 복원 FK collation 오류 | DB를 DROP/CREATE한 뒤 복원 | |
| Terraform 1.16.2가 Secrets Manager 생성 중 패닉 | `terraform import` 6개 | for_each apply가 실패하면 실제 리소스와 대조한다 |

---

# 4부. 설계 선택과 대안

### 1. EC2 사양 상향 vs 구조 변경
- **대안**: t3.small → t3.medium/large 한 대로 유지한다. 비용이 가장 적고 작업량은 0에 가깝다.
- **기각 이유**(포트폴리오 비고): 감시자와 감시 대상이 같은 장애 도메인이라는 문제는 사양으로 해결되지 않는다. 사양을 올리면 한계에 늦게 닿을 뿐이다.
- **치르는 비용**: 운영 복잡도(k3s·ArgoCD·Helm·ESO·Rollouts)가 커졌다. 앱 노드가 오히려 t3.medium이 됐다(컨트롤플레인 몫). 노드는 2대가 됐다.
- ⚠️ **반론에 대비할 것**: 감시자 분리만 따지면 **k3s 없이 CloudWatch 알람만 추가해도 된다**(실제로 84e0fc1에서 구 EC2에 먼저 붙였다). 격리만 따지면 docker compose의 `mem_limit`으로도 된다. k3s를 택한 진짜 이유는 **"선언적 상태 + RDS 비용 제거 + 노드 분리"를 한 번에** 얻기 위해서다. 이렇게 답하는 게 정직하다.

### 2. 단일 EC2 + docker compose + 리소스 제한 vs k3s
- **대안**: compose의 `deploy.resources.limits`, `restart: always`, healthcheck, CloudWatch 알람
- **장점**: 학습 비용이 거의 없고, 노드 하나로 끝난다.
- **기각 이유**: 서버 상태를 git으로 선언하고 자동으로 되돌리는 기능(selfHeal)이 없다. 데이터와 앱을 다른 호스트로 나누려면 결국 여러 호스트를 오케스트레이션해야 한다.
- **비용**: k3s 자체가 메모리를 쓴다(t3.small에서 81MB까지 떨어짐).

### 3. k3s vs EKS vs ECS vs 3노드 HA k3s
- **EKS**: 컨트롤플레인 비용(시간당 과금, 월 약 70달러대 — 확인 필요)이 이 규모의 전체 EC2 비용보다 크다. 대신 IRSA, ECR 연동, 관리형 HA를 얻는다.
- **ECS(Fargate/EC2)**: 컨트롤플레인은 무료이고 IAM 태스크 역할과 ECR 연동이 자연스럽다. 하지만 GitOps 도구(ArgoCD)와 Helm 생태계를 쓸 수 없다. MySQL을 직접 운영하기도 어색하다.
- **3노드 HA k3s**: 컨트롤플레인 SPOF를 없앤다. 하지만 노드가 1대 더 늘어난다(설계 §2에서 비용 대비 과하다고 판단).
- **선택**: 비용 제약이 최우선이었다(설계 §1 목표 5). **비용**: 컨트롤플레인 SPOF, IRSA 없음 → 노드 역할을 모든 파드가 공유한다.

### 4. 관리형 RDS 유지 vs MySQL 파드
- **선택 이유**: RDS 인스턴스 비용 제거(설계 §1 목표 3). 운영 DB가 2MB, 약 4천 행으로 작았다(`README.md:67-69`).
- **잃은 것**: 자동 백업, PITR, Multi-AZ 선택지, 관리형 패치, 스토리지 자동 확장. 대신 매일 mysqldump(RPO ≤ 24h)를 둔다. local-path는 온라인 확장이 안 된다(`apps/mongodb.yaml:47-48` 주석).
- **면접 포인트**: "규모가 작을 때 비용과 위험을 교환한 결정이고, 되돌릴 조건(트래픽·매출)을 정해 뒀다." RDS 최종 스냅샷을 남겼다(Infra `README.md:163-164`).

### 5. 클러스터 안 Prometheus/Grafana vs 클러스터 밖 CloudWatch
- **설계**: 둘 다 쓰는 이중 안전망(§12)
- **실제**: 앱 노드 여유 메모리가 741MB라 클러스터 안 모니터링은 보류했다(`README.md:56-63`). "과거 OOM 장애 패턴이 재현될 위험"이 이유였다.
- **결과**: 지금 감시는 CloudWatch 노드 알람뿐이다. 그중 메모리는 데이터가 없다. 파드와 앱 수준 지표는 없다(앱은 `/actuator/prometheus`를 노출하지만 아무도 수집하지 않는다, `application-prod.yml:94-98`).
- **대안**: CloudWatch Container Insights(유료), 관리형 Prometheus(AMP), 원격 Grafana Cloud 무료 티어, 데이터 노드에 Prometheus 배치 등

### 6. ExternalSecret vs Sealed Secrets vs GitHub Secrets 유지
- **Sealed Secrets**: 암호화한 값을 git에 둔다. 외부 의존성은 없지만 키 관리와 교체가 번거롭다.
- **GitHub Secrets 유지**: CI가 값을 알고 `docker run -e`로 주입하는 구조가 GitOps(클러스터가 스스로 pull)와 맞지 않는다.
- **ESO + Secrets Manager 선택**: 값은 AWS에, 참조는 git에 둔다. 비용은 시크릿당 월 소액이다.
- **비용**: 노드 인스턴스 프로파일이 모든 시크릿 읽기 권한을 가진다. 그래서 **그 노드의 어떤 파드든 IMDS로 읽을 수 있다**(IRSA 없음. IMDS hop limit 설정은 Terraform에 없음 — 확인 필요).

### 7. ECR 토큰 CronJob vs kubelet credential provider
- **CronJob**: 쿠버네티스 리소스로 끝나고 GitOps로 관리된다. 대신 Secret에 토큰이 들어 있고, CronJob 실패와 kubectl 다운로드 의존성이 생긴다.
- **credential provider**: 노드 역할로 kubelet이 직접 인증한다. Secret이 없고 만료 문제도 없다. 대신 노드 부트스트랩(바이너리와 설정)이 필요하다. 이 프로젝트는 노드 부트스트랩을 코드로 관리하지 않는다.
- **선택 근거**: 노드 설정을 수작업으로 늘리지 않고 git 안에서 끝내기 위해서였다. (추정: 커밋 25a8c00 메시지에는 "노드 IAM 역할만으로 안 돼서"만 적혀 있다)

### 8. 순차 컷오버(설계) vs 한 번에 컷오버(실제)
- **설계 §16**: Redis/RabbitMQ → DB → 앱 순서로, 단계별 롤백 기준을 둔다. RDS는 끝까지 유지한다.
- **실제**: 한 번에 전환했다. 운영 DB가 작아 통째로 옮겨도 다운타임이 짧다고 판단했다(`README.md:65-69`).
  - 다운타임은 23:14:54에 시작했다. **종료 시각은 기록이 없다.** 컷오버 기록 커밋(23:30:48)에 health 200을 확인했다고 적혀 있으니 **16분 이내로 추정**한다. DNS TTL이 300초라 클라이언트 쪽은 최대 5분 더 걸릴 수 있다.
  - RDS는 컷오버 약 45분 뒤 최종 스냅샷을 남기고 삭제했다(78b006d). 설계의 "검증 기간 동안 유지"보다 짧다.
- **비용**: 롤백 경로가 "스냅샷에서 RDS 복원"으로 무거워졌다.

---

# 5부. 예상 면접 질문

## 기초

**Q1. 이 장애를 한 문장으로 설명해 주세요.**
> "EC2 한 대에 앱과 미들웨어, 감시 도구까지 모두 있었는데, 저녁에 CPU가 70%로 한 시간 고정된 뒤 OS가 응답하지 않게 됐고, 감시 도구도 같이 죽어서 다음 날 아침까지 약 15시간 동안 아무도 몰랐습니다."
- 꼬리: *15시간은 어떻게 쟀나요?* → CloudWatch 인스턴스 상태 검사가 20:27부터 다음 날 11:34까지 1분 단위로 연속 실패로 남아 있습니다. 첫 실패는 19:36입니다.
- 근거: `CW StatusCheckFailed_Instance`, 84e0fc1

**Q2. 장애 도메인이 뭔가요?**
> "한 원인으로 함께 멈추는 범위입니다. 여기서는 EC2 한 대가 서비스와 감시자를 모두 담고 있어서 하나의 장애 도메인이었습니다."
- 꼬리: *지금 장애 도메인은 몇 개인가요?* → 앱 노드와 데이터 노드 두 개, 그리고 AWS 관리형인 CloudWatch·SNS·Lambda입니다. 다만 앱 노드 하나가 죽어도 서비스 전체가 멈추니, 서비스 입장에서는 여전히 SPOF가 두 개입니다.

**Q3. request와 limit의 차이는요?**
> "request는 스케줄러가 노드에 파드를 놓을 때 쓰는 예약값이고, CPU 경쟁 시 가중치가 됩니다. limit은 상한인데, CPU는 넘으면 스로틀되고 메모리는 넘으면 OOMKilled로 죽습니다."
- 꼬리: *QoS는?* → 둘이 같으면 Guaranteed, request만 있거나 다르면 Burstable, 없으면 BestEffort입니다. 저희 앱은 Burstable이고 cert-manager와 ESO는 BestEffort입니다.
- 근거: `values.yaml:10-15`

**Q4. liveness와 readiness의 차이는요?**
> "readiness가 실패하면 트래픽에서 빠지고, liveness가 실패하면 재시작됩니다. 그래서 liveness는 '프로세스가 회복 불가능하게 멈췄다'만 봐야 하고, 외부 의존성은 readiness에 둬야 합니다."
- 꼬리: *지금 설정은 그렇게 돼 있나요?* → 솔직히 반대입니다. liveness가 DB 상태까지 포함하는 `/actuator/health`를 보고 있어서 데이터 노드가 죽으면 앱이 재시작을 반복합니다. `/actuator/health/liveness`와 `/readiness`로 나눌 계획입니다.
- 근거: `rollout.yaml:57-69`, `TestController.java:40-42`

**Q5. GitOps가 뭔가요? CI/CD와 뭐가 다른가요?**
> "git에 원하는 상태를 선언하고, 클러스터 안 에이전트가 그 상태로 계속 맞추는 방식입니다. 기존 방식은 CI가 서버에 명령을 밀어 넣는 push 방식이었고, GitOps는 클러스터가 git을 끌어오는 pull 방식입니다. 그래서 누가 서버를 손으로 바꿔도 되돌리고, 어떤 커밋이 떠 있는지 git에 남습니다."
- 꼬리: *selfHeal과 prune은?* → selfHeal은 수동 변경을 git 상태로 되돌리고, prune은 git에서 지운 리소스를 클러스터에서도 지웁니다.

**Q6. App-of-Apps는 왜 썼나요?**
> "ArgoCD Application 자체도 git으로 관리하려고 썼습니다. 루트 Application 하나만 처음에 손으로 적용하면, 그 뒤로는 `apps/`에 파일을 추가하는 커밋만으로 새 컴포넌트가 설치됩니다. MongoDB도 그렇게 파일 하나로 추가했습니다."
- 꼬리: *단점은?* → 자식 Application의 values를 바꿔도 자식을 refresh하면 반영되지 않고, 부모(root)를 hard-refresh해야 했습니다. 부모가 자식 스펙을 소유하기 때문입니다.
- 근거: `root.yaml`, 4b58408, `README.md:109-112`

**Q7. k3s가 뭐고 왜 가볍나요?**
> "쿠버네티스를 단일 바이너리로 묶은 배포판입니다. 서버가 한 대면 etcd 대신 SQLite를 쓰고, Traefik, 로컬 스토리지 프로비저너, 서비스 로드밸런서가 기본으로 들어 있습니다. 그래도 t3.small에서는 k3s와 ArgoCD만으로 가용 메모리가 81MB까지 떨어져서 앱 노드를 t3.medium으로 올렸습니다."
- 근거: `README.md:91-95`

**Q8. CPU 크레딧이 뭔가요? 장애와 관계가 있었나요?**
> "t3 인스턴스는 기준 성능 이상으로 CPU를 쓰면 적립한 크레딧을 씁니다. 장애 당시 크레딧 사용량은 올라갔지만 잔고는 576에서 512 정도로만 줄었고 고갈되지 않았습니다. 그래서 크레딧 부족으로 느려진 건 원인이 아니라고 봅니다."
- 꼬리: *크레딧이 고갈되면 어떻게 되나요?* → standard 모드는 기준 성능(vCPU당 20%)으로 묶이고, t3 기본값인 unlimited 모드는 계속 쓰되 초과분이 과금됩니다. 지금 노드들은 unlimited입니다.
- 근거: `CW CPUCreditBalance/Usage/SurplusCreditBalance`, `describe-instance-credit-specifications`

## 구성·코드

**Q9. [필수] 원인 프로세스를 못 찾았는데 어떻게 해결했다고 할 수 있나요?**
> "원인을 고쳤다고 말하지 않습니다. 원인이 무엇이든 같은 결과가 나지 않도록 바꿨다고 말합니다. 첫째, 원인이 메모리든 CPU든 한 컨테이너가 노드를 굳히지 못하게 메모리 limit을 걸었습니다. 둘째, 굳더라도 2분 안에 알 수 있게 클러스터 밖에 상태 검사 알람을 뒀습니다. 이번 장애 지표를 새 알람에 넣어 보면 상태 검사 알람은 19시 38분쯤 울렸을 겁니다."
- 꼬리: *그럼 원인 가설은요?* → 당시 커밋에 '메모리 압박'이라 적었고, 지표 모양도 그 가설과 모순되지 않습니다. CPU가 70%로 고정됐다가 상태 검사가 실패한 뒤 CPU가 거의 0으로 떨어졌고, 스왑이 없었습니다. 다만 메모리 지표가 없어서 확정하지는 못합니다.
- 꼬리: *지금 다시 나면 원인을 찾을 수 있나요?* → 솔직히 아직 부족합니다. k3s 노드에 CloudWatch Agent가 설치돼 있지 않아 메모리 지표가 없습니다. 이걸 먼저 고쳐야 합니다.
- 근거: 84e0fc1, `CW`, 0장 4번

**Q10. [필수] 사양만 올리면 되지 않았나요?**
> "사양을 올리면 한계에 늦게 닿을 뿐, 감시자가 같이 죽는 구조는 그대로입니다. 다만 솔직히 말하면, 감시자 분리만 따지면 CloudWatch 알람을 붙이는 것으로 충분했고 실제로 장애 다음 날 구 EC2에 먼저 붙였습니다. k3s로 간 건 노드 분리, git에 선언된 상태, RDS 비용 제거를 함께 얻으려는 결정이었습니다."
- 근거: 84e0fc1, `design.md:14-19`

**Q11. [필수] 왜 EKS가 아니라 k3s인가요?**
> "비용이 최우선 제약이었습니다. EKS는 컨트롤플레인만으로 시간당 과금이 붙는데, 이 규모에서는 그게 워커 노드 비용보다 큽니다. 대신 k3s는 컨트롤플레인을 제가 운영해야 하고, IRSA가 없어 파드별 IAM 권한을 줄 수 없고, ECR 인증도 직접 해결해야 했습니다."
- 꼬리: *ECS는요?* → ArgoCD와 Helm 생태계를 쓸 수 없고, MySQL을 직접 운영하기 어색해서 뺐습니다.

**Q12. [필수] 노드가 두 대인데 하나가 죽으면 서비스는요?**
> "둘 다 서비스 전체가 멈춥니다. 앱 노드에는 컨트롤플레인, Ingress, 앱이 모두 있고 DNS가 그 노드 IP 하나만 가리킵니다. 데이터 노드에는 DB가 모두 있습니다. 2노드는 가용성이 아니라 장애 격리와 감시를 위한 구성이고, SPOF는 알고 남겨 뒀습니다."
- 꼬리: *데이터 노드가 죽으면 앱은?* → 지금 설정에서는 liveness가 DB 상태를 보고 있어서 재시작을 반복합니다. 개선 과제입니다.
- 근거: `k3s_cluster.tf:197-205`, `design.md:32-33`

**Q13. [필수] 클러스터 전체가 죽으면 누가 알려 주나요?**
> "CloudWatch가 알려 줍니다. AWS 쪽에서 평가하기 때문에 클러스터와 무관합니다. 노드 OS가 굳으면 인스턴스 상태 검사 알람이 2분 안에 SNS, Lambda를 거쳐 Discord로 옵니다. 다만 노드는 살아 있는데 앱만 죽은 경우는 지금 잡지 못합니다. HTTP 헬스체크 알람이 다음 과제입니다."
- 꼬리: *인스턴스를 누가 실수로 stop하면?* → 알람 설정이 '데이터 없음 = 정상'이라 울리지 않습니다. 상태 검사 알람은 missing을 breaching으로 바꾸는 게 맞습니다.
- 근거: `k3s_alarms.tf:13-33`

**Q14. [필수] request와 limit은 어떻게 정했나요? CPU limit은 왜 없나요?**
> "메모리는 장애의 재발 방지가 목적이라 모든 주요 워크로드에 limit을 걸었습니다. 앱은 limit 768Mi에 힙 512MB로 힙 밖 영역을 위해 256Mi를 남겼습니다. CPU limit은 일부러 두지 않았습니다. CPU는 압축 가능한 자원이라 넘쳐도 느려질 뿐 죽지 않고, limit을 걸면 JVM이 GC나 기동 시 스로틀되어 지연이 튑니다. 대신 request로 경쟁 시 비율을 보장했습니다. 값 자체는 실측이 아니라 설계 문서의 '최소로 시작해 지표를 보며 조정' 원칙에 따른 초기값입니다."
- 꼬리: *그런데 포트폴리오에는 CPU 독점을 막았다고 썼는데요?* → 정확히는 '막았다'가 아니라 '경쟁 시 request 비율로 나눈다'입니다. 한 파드가 남는 CPU를 모두 쓰는 건 가능합니다. 표현을 고치겠습니다.
- 꼬리: *request가 없는 파드는?* → cert-manager, ESO, CronJob이 BestEffort입니다. 노드 메모리가 부족하면 먼저 축출되는데, 이것도 보완할 점입니다.
- 근거: 3부 1번 표, `design.md:38-42`

**Q15. [필수] probe가 CI 헬스체크를 대체했다는 게 무슨 뜻인가요?**
> "예전에는 CI가 SSH로 서버에 들어가 5초 간격으로 20번 `/health_check`를 찔러 보고 통과하면 nginx를 바꿨습니다. 지금은 readiness probe가 그 역할을 합니다. 새 파드가 준비될 때까지 Argo Rollouts가 트래픽을 넘기지 않습니다. 차이는 probe가 배포 순간만이 아니라 파드가 사는 동안 계속 본다는 점입니다."
- 꼬리: *잃은 건요?* → CI가 배포 실패를 더는 모릅니다. 예전에는 헬스체크가 실패하면 CI가 빨갛게 표시됐는데, 지금은 CI가 초록으로 끝난 뒤 Rollout이 멈춰도 ArgoCD 화면을 열어야 압니다. ArgoCD Notifications를 붙여야 합니다.
- 근거: old `deploy.yml:155-170`, `rollout.yaml:57-75`

**Q16. [필수] CI 시간이 줄었다는 건 배포가 빨라졌다는 뜻인가요?**
> "아닙니다. CI가 배포를 기다리지 않게 된 것입니다. 배포 잡은 SSH 배포 37~60초에서 GitOps 커밋 5~18초로 줄었지만, CI가 끝난 뒤에도 ArgoCD 감지와 파드 기동, probe 통과가 남아 있습니다. CI 전체 시간은 전환 전후 150~190초대로 비슷합니다. 그 사이 테스트가 늘어 빌드가 길어졌기 때문입니다."
- 꼬리: *그럼 787초, 869초는 뭐였나요?* → 실행 기록을 다시 보니 둘 다 재실행이었습니다. 첫 시도부터 재실행까지의 대기 시간이 합쳐진 값이라 서버 지연과는 관계없었습니다.
- 꼬리: *배포 완료까지 걸리는 시간은 쟀나요?* → 아직 안 쟀습니다. 커밋 시각부터 Rollout Healthy 시각까지를 ArgoCD 이벤트로 재야 합니다.
- 근거: 2부 1번 표

**Q17. [필수] RDS를 걷어낸 게 위험하지 않나요?**
> "위험합니다. 그래서 조건을 두고 감수했습니다. 운영 DB가 2MB, 4천 행 수준이라 RDS 비용이 데이터 가치에 비해 컸습니다. 대신 매일 새벽 3시 mysqldump를 S3에 올리고 30일 보관합니다. 잃은 건 PITR, 자동 장애 조치, 관리형 패치입니다. 최악의 경우 하루 치 쓰기를 잃을 수 있습니다. 아직 복원 리허설을 안 한 건 인정합니다."
- 꼬리: *백업이 실패하면 아나요?* → 지금은 모릅니다. Job 실패 알림이 없습니다.
- 근거: `mysql-backup/cronjob.yaml:13-14`, `lambda_s3_lifecycle.tf:14-25`

**Q18. [필수] ECR 토큰 CronJob이 멈추면요?**
> "토큰은 12시간 유효하고 6시간마다 갱신하니까, 한 번 실패는 버팁니다. 두 번 연속 실패하면 만료되는데, 그래도 떠 있는 파드는 이미 이미지를 받아 두어 영향이 없습니다. 깨지는 건 다음 배포입니다. 새 이미지를 못 받아 Rollout이 멈추고, blue/green이라 옛 버전이 계속 서비스합니다. 문제는 그걸 알려 주는 곳이 없다는 겁니다."
- 꼬리: *더 나은 방법은?* → kubelet credential provider로 노드 역할을 직접 쓰면 Secret과 CronJob이 필요 없습니다.
- 근거: `cronjob.yaml:7-9`

**Q19. [필수] 장애 시간 4시간은 어떻게 측정했나요?**
> (정정된 답) "처음에는 기억으로 4시간이라고 적었는데, CloudWatch 상태 검사 기록을 다시 확인해 보니 20시 27분부터 다음 날 11시 34분까지 약 15시간 연속 실패였습니다. 알람 추가 커밋에도 '약 14시간 방치'라고 남아 있어서 15시간으로 바로잡았습니다."
- 꼬리: *사용자 입장의 장애 시간은요?* → HTTP 요청 기록이 없어서 정확히는 모릅니다. 인스턴스가 응답 불능이었던 시간이 하한 근사입니다.
- 근거: `CW`, 84e0fc1

**Q20. ECR 인증 CronJob에 최소 권한을 줬다고 했는데 구체적으로요?**
> "ServiceAccount 하나에 `forday-app` 네임스페이스의 Secret에 대한 get, create, update, patch만 주는 Role을 바인딩했습니다. 클러스터 전체 권한이나 delete, list는 없습니다."
- 꼬리: *그 Role로 앱 시크릿도 읽을 수 있지 않나요?* → 맞습니다. `resourceNames`로 `ecr-pull-secret`만 지정하지 않아서 같은 네임스페이스의 `forday-app-secrets`도 읽을 수 있습니다. 게다가 AWS 권한은 노드 역할 전체에서 옵니다. 고칠 부분입니다.
- 근거: `rbac.yaml:11-19`, `k3s_cluster.tf:99-140`

**Q21. 시크릿을 Secrets Manager로 일원화했다는데, 남은 건 없나요?**
> "Firebase 서비스 계정 JSON이 아직 GitHub Secret이고 빌드 때 jar 안에 들어갑니다. S3 접근도 인스턴스 역할이 아니라 정적 액세스 키입니다. 옮긴 건 DB, JWT, OpenAI, Apple, S3 키, RabbitMQ, MongoDB이고, RabbitMQ 비밀번호는 원래 워크플로에 평문으로 있던 걸 이번에 시크릿으로 옮겼습니다."
- 근거: new `deploy.yml:38-43`, old `deploy.yml:141-142`, `forday-app-external-secret.yaml`

**Q22. 시크릿 값을 바꾸면 언제 반영되나요?**
> "ESO가 1시간 안에 k8s Secret을 갱신하지만, 앱이 환경변수로 읽기 때문에 파드를 재시작해야 반영됩니다. DB 비밀번호는 DB 안의 계정도 같이 바꿔야 합니다."

**Q23. 메모리 알람은 어떻게 동작하나요?**
> (정직한 답) "구 EC2에서는 CloudWatch Agent가 `ForDay/EC2` 네임스페이스로 `mem_used_percent`를 보내고, 85%가 10분 지속되면 울리게 했습니다. k3s 노드에도 같은 알람을 만들었지만, 확인해 보니 Agent를 설치하지 않아 데이터가 한 번도 들어오지 않았습니다. 데이터 없음을 정상으로 처리하는 설정이라 계속 OK로 보였습니다. 지금은 8개 중 6개만 실제로 동작합니다."
- 근거: 0장 4번

**Q24. blue/green에서 전환 기준은 뭔가요? 잘못된 버전이 나가면?**
> "새 ReplicaSet의 readiness가 통과하면 자동 전환합니다. 이미지가 없거나 안 뜨면 전환되지 않고 옛 버전이 유지됩니다. 그런데 뜨기는 하는데 로직이 깨진 버전은 `/health_check`가 항상 200이라 그대로 전환됩니다. 30초 안에는 abort로 되돌릴 수 있고, 그 뒤에는 git revert로 되돌립니다. 에러율 기반 자동 판단(Analysis)은 아직 없습니다."
- 근거: `values.yaml:44-48`

**Q25. 배포 중 메모리는 충분한가요?**
> "blue/green이라 전환 중에 앱 파드가 두 개 뜨고, 각각 512Mi request와 768Mi limit이 있습니다. Phase 3에서 앱 노드 여유를 741MB로 기록했으니 여유가 크지 않습니다. 그래서 클러스터 안 모니터링도 보류했습니다."
- 근거: `README.md:56-63`, `design.md:87`

**Q26. 컷오버는 어떻게 했고 다운타임은 얼마였나요?**
> "원래는 컴포넌트별 순차 이전을 설계했지만, DB가 2MB로 작아 한 번에 했습니다. 구 blue 컨테이너를 23시 14분 54초에 멈춰 쓰기를 막고, RDS에서 덤프해 새 MySQL에 복원한 뒤 DNS A 레코드를 바꿨습니다. 종료 시각은 따로 기록하지 않았고, 16분 뒤 커밋한 기록에 헬스체크 200을 확인했다고 남아 있습니다."
- 꼬리: *복원 중 문제는?* → Hibernate가 미리 만든 빈 스키마의 collation이 덤프와 달라 FK 오류가 났습니다. DB를 DROP하고 다시 만든 뒤 복원했습니다.
- 근거: `README.md:65-81, 120-124`

## 꼬리질문·압박

**Q27. 장애 다음 날 하루 저녁에 설계부터 컷오버, RDS 삭제까지 했던데 너무 서두른 것 아닌가요?**
> "기록상 그렇습니다. 20시에 설계 문서, 23시 14분에 컷오버, 자정에 RDS 삭제입니다. 서비스 규모가 작고 RDS 최종 스냅샷을 남겨서 롤백이 가능하다고 판단했습니다. 다만 설계에서 정한 '검증 기간 동안 RDS 유지'보다 짧았고, 복원 리허설 없이 진행한 점은 지금 보면 아쉽습니다."
- 근거: 타임라인, 78b006d

**Q28. 설계 문서와 실제가 다른 부분이 있나요?**
> "세 가지입니다. 데이터 워크로드는 수동 sync로 하기로 했는데 자동 sync에 prune까지 켜져 있습니다. 클러스터 안 모니터링은 메모리 여유 때문에 보류했습니다. 순차 컷오버 대신 한 번에 전환했습니다. 앞의 것은 되돌려야 하고, 나머지 두 개는 근거를 기록해 둔 의도적 변경입니다."
- 근거: `design.md:75`, `apps/mysql.yaml:43-50`

**Q29. prune이 켜진 MySQL Application이 잘못된 커밋으로 지워지면 데이터는요?**
> "StatefulSet이 삭제돼도 volumeClaimTemplate으로 생긴 PVC는 보통 ArgoCD 관리 대상이 아니라 남습니다. 그래도 그 동작에 기대는 건 위험해서, 설계대로 데이터 Application은 prune을 끄는 게 맞습니다. 확인해 보겠습니다."

**Q30. CPU가 70%인데 왜 서버가 멈췄나요? 100%도 아니잖아요.**
> "그 점이 제가 '메모리 쪽을 의심한다'고 말하는 이유입니다. CPU가 포화되지 않았는데 상태 검사가 실패했고, 그 뒤 CPU가 1%까지 떨어진 채 멈춰 있었습니다. 스왑 없는 메모리 고갈에서 커널이 페이지 회수에 CPU를 쓰다가 결국 굳는 모양과 비슷합니다. 하지만 메모리 지표가 없어서 가설로만 말합니다."
- 근거: `CW CPUUtilization 5분`, 84e0fc1(스왑 2GB 추가)

**Q31. 당시 Prometheus/Grafana가 있었는데 사후에 그 데이터를 못 봤나요?**
> (본인 확인 필요) "Prometheus가 같은 서버 디스크에 데이터를 쓰고 있었다면 재부팅 뒤 볼 수 있었을 수 있습니다. 당시 확인하지 않았고 보존 설정도 기억나지 않습니다. 이번 교훈이 '관측 데이터도 서비스와 다른 곳에 저장해야 한다'는 것입니다."
- ⚠️ 저장소에 운영 Prometheus 설정이 없다(`docker/perf/`는 측정용). **기억에 기반한 답이라는 점을 밝힌다.**

**Q32. 상태 확인이 2분에서 10초로 줄었다는 건 어떻게 쟀나요?**
> "재지 않았습니다. 체감 수치라 포트폴리오에서는 빼거나 '체감'으로 표시하겠습니다. 비교한다면 SSH 접속 후 `docker ps`까지의 시간과 ArgoCD 화면 로딩 시간을 녹화해서 재겠습니다."

**Q33. ArgoCD UI를 인터넷에 노출했는데 괜찮나요?**
> "`argocd.forday.kr`로 TLS를 붙여 노출했습니다. 관리자 로그인이 공개 인터넷에 열려 있는 셈이라, IP 제한 미들웨어나 SSO, 또는 포트포워딩으로만 접근하게 바꾸는 게 맞습니다."
- 근거: `bootstrap/argocd-ingress.yaml`

**Q34. 알람이 Discord로 온다는 건 검증했나요?**
> "SNS 토픽에 테스트 메시지를 발행해 Discord 수신까지 확인했습니다. 처음에는 Discord가 기본 User-Agent를 403으로 막아서 헤더를 추가했습니다. 다만 알람 상태를 실제로 ALARM으로 바꿔서 끝까지 흘려 본 기록은 없어서, `set-alarm-state`로 한 번 더 확인하겠습니다."
- 근거: Infra `README.md:120-122`, `index.py:65-67`

**Q35. 이 구성에서 가장 먼저 고칠 세 가지는?**
> "첫째, k3s 노드에 CloudWatch Agent를 설치하고 HTTP 헬스체크 알람을 추가하겠습니다. 감시 공백이 가장 큽니다. 둘째, liveness와 readiness 경로를 바꾸겠습니다. 데이터 노드 장애가 앱 재시작 폭주로 번지는 걸 막기 위해서입니다. 셋째, 백업 복원 리허설과 백업 실패 알림을 붙이겠습니다."

---

# 6부. 포트폴리오 문장 점검

| # | 원문 | 판정 | 근거 | 수정안 |
|---|---|---|---|---|
| 1 | 제목 "서버 한 대가 **4시간** 멈춘 날" | **불일치** | 상태 검사 연속 실패 20:27~11:34(15h07m), 커밋 "약 14시간" | "서버 한 대가 **15시간** 멈춘 날" |
| 2 | 문제1 "CPU 사용률이 **100% 가까이 고정**된 채 응답 불능" | **불일치** | 5분 평균 최대 약 70%, 최댓값 80.4%. 상태 검사 실패 이후 약 1% | "CPU가 약 70%로 1시간 고정된 뒤 인스턴스 상태 검사가 실패했고, 이후 CPU가 거의 0인 채로 응답이 없었습니다" |
| 3 | 문제1 "**약 4시간** 서비스가 중단" | 불일치 | 같음 | "다음 날 재부팅까지 **약 15시간**" |
| 4 | 문제2 "감시 체계가 서비스와 함께 죽었다" | 사실 | `design.md:9, 12` | 유지 |
| 5 | 문제4 "EC2 기본 제공 지표만 있었고, 메모리 지표는 이 장애를 계기로 추가" | 사실 | 84e0fc1, `mem_used_percent` 첫 데이터 09-17 11시대 | 유지. 캡처 4번으로 증거를 확보한다 |
| 6 | 문제4 "사후 확인 가능한 건 CPU 사용률과 **CPU 크레딧**… CPU 사용률은 **100%에 가깝게**" | 부분 불일치 | 크레딧 잔고 고갈 없음(≥512/576), 사용률 약 70% | "CPU 사용률(약 70% 고정)과 크레딧 사용량은 볼 수 있었지만 크레딧 잔고는 충분했습니다" |
| 7 | 원인1 "한 프로세스가 노드의 CPU·메모리를 전부 가져가는 것을 막는 장치가 없었다" | 추론(타당) | 원인 미특정과 모순되지 않게 "~을 막을 장치가 없었다"로 둔 것은 적절 | 유지 |
| 8 | 측정 "알람이 없어 장애 중에는 아무도 알지 못했다" | 사실 | 84e0fc1 "알람이 하나도 없어" | 유지 |
| 9 | 측정 "SSH 배포(구) **25회** 대부분 **145~230초**, 최댓값 **787·869초**" | **부정확** | 25회 안에 9223·411초 포함, 최솟값 142초. 1차 시도 성공은 142~195초. 787은 실패 런 | "1차 시도 기준 142~195초. 튄 값(411~9223초)은 모두 재실행이라 제외" |
| 10 | 측정 "서버 쪽 지연이 CI 시간에 그대로 더해졌습니다… 튄 구간의 나머지 지연 원인은 특정하지 못했습니다" | **틀림** | 튄 런은 모두 attempt=2, 대기 261~9087초. 배포 잡은 37~60초 | 삭제. 대신 "배포 잡이 헬스체크 루프를 포함해 37~60초, 실패 시 113~118초를 CI 안에서 기다렸다" |
| 11 | 측정 "헬스체크(`for i in {1..20}`, 최대 100초 대기)" | 사실 | old `deploy.yml:155-170`, 실패 배포 잡 113·118초 | 유지(실측 근거 추가 가능) |
| 12 | 측정 "전환 후 4회 표본 **151~193초**" | 사실 | 916e7dd 151, c56b904 153, 03f843b 172, 6746dae 193 | 유지. 단 전체 시간은 줄지 않았다는 점을 함께 밝힌다 |
| 13 | 측정 "서버 상태 확인 약 2분 → 10초 안팎" | **증거 없음** | 기록 없음 | 삭제하거나 "체감". 남기려면 녹화 |
| 14 | 해결1 "컨테이너마다 request/limit을 명시해 CPU·메모리 독점 못 하게" | **과장** | CPU limit 없음. BestEffort 4종 | "주요 워크로드에 메모리 limit과 CPU·메모리 request를 걸어, 메모리 폭주가 노드 전체로 번지지 않게 했습니다" |
| 15 | 해결2 "노드 2대마다 상태검사·CPU·메모리 알람 4종, 총 8개" | 정의는 사실, **동작은 6개** | `k3s_alarms.tf`, 메모리 지표 미수집 | "8개를 정의했고, 메모리 2개는 Agent 설치가 남아 있습니다" 또는 Agent를 설치한 뒤 유지 |
| 16 | 해결2 "메모리는 커스텀 지표(`mem_used_percent`)로 수집해 알람에 걸었습니다" | **k3s 노드에서는 틀림** | `list-metrics` | 설치 후 유지 |
| 17 | 해결3 "응답 불능 컨테이너를 재시작, 준비 안 된 컨테이너에 트래픽 차단 → CI 20회 헬스체크 대체" | 사실(앱 한정) | `rollout.yaml:57-69` | "앱 컨테이너에" 한정어 추가. liveness 경로 문제는 면접에서 먼저 언급 |
| 18 | 해결4 "`apps/`에 Application 파일 하나만 추가하면 ArgoCD가 자동 인식" | 사실 | `root.yaml`, 4b58408 | 유지 |
| 19 | 해결5 "6시간마다 ECR 토큰(12시간 유효)… CronJob" | 사실 | `cronjob.yaml:7-8` | 유지 |
| 20 | 해결5 "ServiceAccount·Role·RoleBinding으로 **최소 권한만**" | 과장 | `resourceNames` 없음 → 앱 시크릿도 읽기 가능 | "네임스페이스 범위 Secret 쓰기 권한으로 제한" 또는 `resourceNames`를 추가한 뒤 유지 |
| 21 | 해결6 "GitHub Actions Secrets **15개** 제거" | **불일치** | 옛 참조 15, 현재 참조 4(3 유지 + 1 신규) → **12개 제거** | "15개 중 12개를 걷어냈습니다" |
| 22 | 해결6 "Secrets Manager로 모으고" | 부분 | FIREBASE_JSON은 GitHub에 남아 이미지에 포함 | "앱 런타임 시크릿 12개 키를 모으고"로 범위 한정 |
| 23 | 해결6 "RDS 자동 백업을 예약 작업 + S3로 대체" | 사실 | 매일 03:00 KST, 30일 | "매일 1회(RPO 최대 24시간), 30일 보관"을 명시. 복원 미검증 |
| 24 | 평가 "관측 가능성: 서버 상태가 git 커밋으로 항상 선언·조회 가능" | 대체로 사실 | 예외: ArgoCD 설치·patch, Traefik HelmChartConfig, GRANT PROCESS는 git 밖 | "애플리케이션과 데이터 컴포넌트는" 한정 |
| 25 | 비고 "감시자를 클러스터 밖 CloudWatch로 옮겼다" | 사실, 범위 누락 | 클러스터 안 모니터링 보류, HTTP 감시 없음 | "노드 수준 감시를" 추가 |
| 26 | 비고 "2노드로 최소한만 바꾼 이유는 비용" | 사실 | `design.md:19, 32` | 유지 |
| 27 | (누락) 설계는 순차 컷오버, 실제는 일괄 | 누락 | `README.md:65-69` | 비고에 한 줄 추가 권장 |
| 28 | (누락) 컷오버 다운타임 | 기록 부분 | 시작 23:14:54만 기록, 종료 미기록(≤16분 추정) | 쓰려면 "약 16분 이내"로 |

---

# 외워둘 숫자

| 항목 | 값 | 출처 |
|---|---|---|
| 장애 시작 신호 | 09-16 18:40 CPU 상승, **19:36 상태 검사 첫 실패** | CW |
| 장애 연속 구간 | **20:27 ~ 다음 날 11:34, 15시간 7분** | CW StatusCheckFailed_Instance 1분 |
| 당시 CPU | 5분 평균 최대 약 **70%**(1시간 고원), 최댓값 80.4%, 이후 약 1% | CW |
| 크레딧 잔고 | 576 → 최저 약 **512**, 초과 0(고갈 없음) | CW |
| 알람 추가 커밋 | 09-17 11:48, "약 14시간 방치" | Infra 84e0fc1 |
| 알람 | 노드 2 × 4 = **8개 정의 / 6개 동작**(메모리 2개 데이터 없음) | `k3s_alarms.tf`, CW |
| 알람 조건 | 상태 검사 60초×2, CPU 85%·5분×3, 메모리 85%·5분×2 | `k3s_alarms.tf`, `variables.tf:68-82` |
| 노드 | 앱 **t3.medium**(4GiB, k3s server) / 데이터 **t3.small**(2GiB, agent), 둘 다 unlimited | `terraform.tfvars:31`, `variables.tf:205-209`, `describe-instances` |
| t3.small 앱 노드 실패 | 가용 **81MB**, API 서버 응답 불가 | GitOps `README.md:91-95` |
| 앱 노드 여유 | **741MB**(Phase 3) → 모니터링 보류 | GitOps `README.md:59-61` |
| 앱 자원 | request cpu **250m** / mem **512Mi**, limit mem **768Mi**, `-Xmx512m` | `values.yaml:10-20` |
| probe | readiness `/health_check` 20s/5s/×6, liveness `/actuator/health` 40s/15s/×3(기본) | `rollout.yaml:57-69` |
| Rollout | autoPromotion true, scaleDownDelay **30s** | `values.yaml:44-48` |
| 옛 헬스체크 | 5초 × **20회**(최대 100초). 실패 배포 잡 **113·118초** | old `deploy.yml:156-164`, gh |
| CI 전(1차 시도 성공 21회) | **142~195초**, 배포 잡 37~60초 | gh |
| CI 전 재실행 4회 | 411 / 787(실패) / 869 / 9223초 | gh |
| CI 후(4회) | **151~193초**, update-gitops 5~18초 | gh |
| ECR | 토큰 **12시간**, 갱신 **6시간**(`0 */6 * * *`) | `cronjob.yaml:7-8` |
| 백업 | 매일 **03:00 KST**(`0 18 * * *` UTC), S3 `db-backups/mysql/`, **30일** 만료 | `mysql-backup/cronjob.yaml:13-14`, `lambda_s3_lifecycle.tf:22-24` |
| PV | MySQL 10Gi, RabbitMQ 4Gi, MongoDB 5Gi, 데이터 노드 디스크 40GB | `apps/*.yaml`, `variables.tf:217-221` |
| 시크릿 | GitHub 참조 **15 → 4**(12개 제거, GITOPS_PAT 1개 추가). Secrets Manager 그릇 6 → 7개, 앱 키 12개 | old/new `deploy.yml`, `gh secret list`, `k3s_secrets.tf` |
| ESO 갱신 | **1h**(파드 반영은 재시작 필요) | `forday-app-external-secret.yaml:12` |
| 컷오버 | **2026-09-17 23:14:54 KST**(14:14:54 UTC) 시작, 기록 커밋 23:30:48, DNS TTL 300초 | GitOps `README.md:71-79`, 771062d |
| 구 인프라 삭제 | 09-18 00:00(컷오버 약 45분 뒤), RDS 최종 스냅샷 `forday-rds-final-20260917` | Infra 78b006d |
| 운영 DB 크기(당시) | 약 **2MB / 4천 행** | GitOps `README.md:67-69` |
