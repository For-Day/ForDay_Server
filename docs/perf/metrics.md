# 지표 노출 (이슈 #373)

이 문서는 `#373`에서 연 `/actuator` 엔드포인트가 무엇을 노출하는지, 어떻게 확인하는지를 담는다.
대시보드나 별도 측정 환경 구축은 다루지 않는다 — 그건 `#374`(부하 테스트 전용 측정 환경 구축)의
범위다. 여기서 노출한 지표는 이후 `#374`·`#376`이 원인 분석에 쓴다.

## 노출 엔드포인트

`management.endpoints.web.exposure.include: health,prometheus`로 이 둘만 연다. 그 외
actuator 하위 경로(`env`, `beans`, `heapdump` 등)는 노출 목록에 없어 `SecurityConfig`의
`anyRequest().authenticated()`로 떨어지고, JWT 없이는 403으로 거절된다 — Spring MVC가
"매핑된 경로가 없다"고 판단하기(404) 전에 Security 필터가 먼저 막는다(`ActuatorEndpointAccessTest`가 고정).

| 엔드포인트 | 용도 |
| --- | --- |
| `/actuator/health` | 배포 헬스체크와는 별개로, 애플리케이션이 떠 있는지 빠르게 확인 |
| `/actuator/prometheus` | Prometheus 텍스트 포맷으로 아래 모든 지표를 한 번에 노출 |

`SecurityConfig`가 이 두 경로를 JWT 없이 열어둔다 — **실제 접근 제어는 nginx가 맡는다.**
아래 [외부 접근 차단](#외부-접근-차단-nginx-아직-미적용) 참고.

## 자동 수집되는 지표

`spring-boot-starter-actuator` + `micrometer-registry-prometheus`가 코드 변경 없이 자동으로
계측한다.

| 지표 | 확인 방법 |
| --- | --- |
| HikariCP 활성·대기 커넥션 | `curl .../actuator/prometheus \| grep hikaricp_connections` |
| JVM GC 횟수·시간 | `curl .../actuator/prometheus \| grep jvm_gc` |
| HTTP 요청 p50/p95/p99 | `curl .../actuator/prometheus \| grep http_server_requests` |

`http.server.requests`의 percentile은 `management.metrics.distribution.percentiles.http.server.requests: 0.5,0.95,0.99` 설정으로 인스턴스 로컬 값을 직접 노출한다. 여러 인스턴스(블루-그린)를 PromQL로 합산 집계해야 하는 시점이 오면(`#374` 이후) `percentiles-histogram: true`로 바꿔 히스토그램 버킷을 쓴다 — 지금은 "값이 잡히는지 확인" 수준이라 과설계하지 않았다.

## 커스텀 지표

| 지표 | 설명 |
| --- | --- |
| `reaction_queue_size` | Redis `reaction_queue` 리스트의 현재 길이(`ReactionQueueMetrics`). `ReactionScheduler`가 1초마다 최대 1000건씩 비운다 — 유입률이 이 상한을 넘으면 계속 늘어난다. `#376` 스파이크 테스트가 포화 유입률을 찾을 때 이 지표로 판단한다. |

## 확인 명령

```bash
# 로컬(local 프로파일)에서 bootRun 후
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus | grep reaction_queue_size
```

## 테스트 작성 시 겪은 함정

`ActuatorEndpointAccessTest`를 만들면서 겪은, 문서화 안 해두면 다음에 또 반나절 날릴 문제
두 가지.

**1. `@SpringBootTest`는 메트릭 발행을 기본적으로 막는다.** `application-test.yml`에
`management.endpoints.web.exposure.include: health,prometheus`를 적어도, 실제로 물어보면
`prometheus`가 빠진 채로 읽힌다. 원인은 Spring Boot 테스트 인프라의
`ObservabilityContextCustomizerFactory`가 `management.defaults.metrics.export.enabled=false`를
파일 설정보다 우선순위가 높은 합성 프로퍼티 소스로 주입하기 때문이다 — 테스트가 실제 메트릭을
외부로 새어나가지 않게 막는 의도된 안전장치다. `application-test.yml`을 고쳐도,
`@SpringBootTest(properties = "...")`로 덮어써도 안 먹힌다(둘 다 직접 확인). 해제하는
공식적인 방법은 테스트 클래스에 `@AutoConfigureObservability`를 붙이는 것뿐이다.

**2. RabbitMQ 없는 환경에서 `/actuator/health`는 503이 난다.** `RabbitHealthIndicator`가
기본으로 켜져 있어 브로커 연결을 시도하고, 실패하면 전체 health 상태가 DOWN(503)으로
떨어진다. `deploy.yml`의 CI가 `redis` 서비스 컨테이너만 띄우고 RabbitMQ는 띄우지 않으므로,
이 테스트를 처음 추가한 순간 CI에서도 똑같이 503이 났을 것이다. `application-test.yml`에
`management.health.rabbit.enabled: false`로 테스트 프로파일에서만 껐다 — 실제 배포
환경에서는 이 값을 설정하지 않아 RabbitMQ 상태가 `/actuator/health`에 그대로 반영된다
(블루-그린 헬스체크가 쓰는 `/health_check`는 이 인디케이터와 무관한 별개 엔드포인트라
영향받지 않는다).

## 설정 — 이 저장소가 직접 담을 수 없는 부분

`application.yml`(로컬)과 `application-prod.yml`(배포용 `APPLICATION_PROPERTIES` GitHub Secret의
로컬 참고 사본)은 둘 다 `.gitignore` 대상이라 이 PR이 값을 바꿀 수 없다. 아래 블록을 두 곳에
직접 추가해야 한다.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,prometheus
  metrics:
    distribution:
      percentiles:
        http.server.requests: 0.5,0.95,0.99
```

`application-prod.yml`에는 이미 `management.endpoints.web.exposure.include: health,env`가 있다.
**`env`는 설정값을 그대로 노출해 화이트리스트에 없느니만 못하다 — `prometheus`로 교체할 것**
(`env`를 함께 남기지 않는다).

## 외부 접근 차단 (nginx, 아직 미적용)

nginx 설정은 이 저장소 어디에도 버전 관리되지 않는다 — 배포 워크플로가 SSH로 EC2에 붙어
`/etc/nginx/conf.d/service-env.inc` 한 줄만 직접 쓸 뿐, 메인 서버 블록은 EC2 위에만 있다.
그래서 아래는 실제 서버 블록 구조를 EC2에서 확인한 뒤 직접 끼워 넣어야 하는 **참고 스니펫**이다.

```nginx
location /actuator {
    allow 127.0.0.1;
    # allow <내부/특정 IP 대역>;
    deny all;
    proxy_pass http://$service_url;   # 기존 blue/green 라우팅과 동일하게
}
```

적용 후 확인: 외부(EC2 바깥)에서 `curl https://<도메인>/actuator/prometheus`가 403이어야 하고,
내부 헬스체크·스크래핑 경로에서는 정상 응답해야 한다.

**현재 상태**: EC2 SSH 접속이 막혀 있어(TCP는 연결되나 SSH 배너 응답 없음 — sshd 응답 불능
추정) 이 단계는 적용하지 못했다. 접속 복구 후 별도로 처리한다.
