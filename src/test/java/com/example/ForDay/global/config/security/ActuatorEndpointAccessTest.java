package com.example.ForDay.global.config.security;

import com.example.ForDay.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code SecurityConfig}를 검증하는 첫 테스트다(이전까지 이 코드베이스에 없었다).
 * 이슈 #373이 요구하는 두 가지를 함께 고정한다 — (1) 스크래핑 대상 엔드포인트는 JWT 없이도
 * 열려 있고, (2) 노출 목록(health,prometheus)에 없는 엔드포인트는 접근이 막힌다.
 *
 * <p>{@code IntegrationTestSupport}의 {@code @SpringBootTest}는 기본 MOCK 웹 환경이라 실제
 * 포트가 없다. 여기서는 실제 HTTP 요청이 필요해 {@code webEnvironment = RANDOM_PORT}로
 * 직접 재선언한다 - 서브클래스에 명시한 {@code @SpringBootTest}가 상위 클래스 것을 완전히
 * 대체하므로(병합되지 않음) 이 재선언만으로 충분하다.
 *
 * <p>{@code @AutoConfigureObservability}가 필요한 이유 — Spring Boot 테스트 인프라는
 * {@code @SpringBootTest} 실행 시 {@code ObservabilityContextCustomizerFactory}로 실제
 * 메트릭·트레이싱이 외부로 새나가지 않도록 {@code management.defaults.metrics.export.enabled}를
 * 자동으로 {@code false}로 주입한다. 이 합성 프로퍼티 소스는 {@code application-test.yml}
 * 파일은 물론 {@code @SpringBootTest(properties=...)}보다도 우선순위가 높아 둘 다로는
 * 못 이긴다(직접 확인함). {@code @AutoConfigureObservability}가 이 차단을 해제하는,
 * Spring Boot가 제공하는 공식적인 방법이다 — 이게 없으면 {@code /actuator/prometheus}가
 * {@code PrometheusMeterRegistry} 빈 부재로 404 대신 {@code NoResourceFoundException}(500)을
 * 낸다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureObservability
class ActuatorEndpointAccessTest extends IntegrationTestSupport {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("/actuator/health는 JWT 없이도 200을 반환한다")
    void health는_인증_없이_접근된다() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("/actuator/prometheus는 JWT 없이도 200을 반환하고 reaction_queue_size 게이지를 포함한다")
    void prometheus는_인증_없이_접근되고_커스텀_게이지를_포함한다() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/prometheus", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("reaction_queue_size");
    }

    @Test
    @DisplayName("노출 목록에 없는 actuator 엔드포인트는 막힌다 - health,prometheus로 제한이 실제로 걸려 있다")
    void 노출되지_않은_엔드포인트는_막힌다() {
        // SecurityConfig가 permitAll로 열어준 건 /actuator/health, /actuator/prometheus뿐이다.
        // /actuator/env는 목록에 없어 anyRequest().authenticated()로 떨어지고, Spring MVC가
        // "매핑된 경로가 없다"고 판단하기(404) 전에 Spring Security 필터가 먼저 막아
        // 403으로 거절한다 - actuator 노출 설정과 무관하게 걸리는 별도의 방어선이다.
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/env", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
