package com.example.ForDay.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 배포 사고 재발 방지용 회귀 테스트 (이슈 #390 후속).
 *
 * <p>application.yml의 {@code spring.profiles.active: local}이 무조건 활성화되는
 * 바람에, 배포 시 SPRING_PROFILES_ACTIVE=green을 줘도 "local"이 함께 활성화되어
 * (blue/green 전용 설정이 아니라) 로컬 설정값이 우선 적용되는 사고가 있었다.
 * {@code spring.profiles.default}로 바꿔서 외부에서 활성 프로필을 지정하면
 * "local"이 전혀 끼어들지 않도록 고쳤다.
 */
@Configuration
class ActiveProfileResolutionTest {

    @Test
    @DisplayName("외부에서 활성 프로필(green)을 지정하면 local이 함께 활성화되지 않는다")
    void 외부_프로필_지정시_local이_섞이지_않는다() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(ActiveProfileResolutionTest.class)
                .web(WebApplicationType.NONE)
                .run("--spring.profiles.active=green")) {

            assertThat(context.getEnvironment().getActiveProfiles())
                    .contains("green")
                    .doesNotContain("local");

            // application-prod.yml이 실제로 로드되는지, "green"이 profiles.group을
            // 통해 "common"까지 끌고 오는지를 프로필 이름이 아니라 실제 프로퍼티
            // 값으로 검증한다 - 배포 사고 당시 여기가 진짜 문제였는지 확인하기 위해.
            assertThat(context.getEnvironment().getProperty("server.env")).isEqualTo("green");
            assertThat(context.getEnvironment().getProperty("serverName")).isEqualTo("green_server");
        }
    }

    @Test
    @DisplayName("외부에서 아무 프로필도 지정하지 않으면 local 기본값으로 뜬다")
    void 프로필_미지정시_local이_기본값이다() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(ActiveProfileResolutionTest.class)
                .web(WebApplicationType.NONE)
                .run()) {

            assertThat(context.getEnvironment().getProperty("server.env")).isEqualTo("local");
        }
    }
}
