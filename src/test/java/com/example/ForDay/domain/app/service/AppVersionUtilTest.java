package com.example.ForDay.domain.app.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppVersionUtilTest {

    @Test
    @DisplayName("버전 문자열 파싱 테스트 - 정상 케이스 및 유연한 처리")
    void versionParsingTest() {
        // Given & When
        AppVersionUtil v1 = AppVersionUtil.of("1.2.3", 100);
        AppVersionUtil v2 = AppVersionUtil.of("1.2", 100); // 패치 생략
        AppVersionUtil v3 = AppVersionUtil.of("2-beta", 100); // 문자 포함

        // Then
        assertThat(v1.versionString()).isEqualTo("1.2.3");
        assertThat(v2.versionString()).isEqualTo("1.2.0");
        assertThat(v3.versionString()).isEqualTo("2.0.0");
    }

    @ParameterizedTest
    @DisplayName("버전 비교 테스트 - 다양한 시나리오")
    @CsvSource({
            "1.0.0, 100, 1.0.1, 100, -1", // Patch가 높으면 뒤가 더 큼
            "1.1.0, 100, 1.0.9, 100, 1",  // Minor가 높으면 앞이 더 큼
            "2.0.0, 1,   1.9.9, 999, 1",  // Major가 높으면 빌드 상관없이 앞이 더 큼
            "1.0.0, 200, 1.0.0, 100, 1",  // 버전이 같으면 빌드 번호로 비교
            "1.2.3, 100, 1.2.3, 100, 0"   // 완전히 같으면 0
    })
    void versionComparisonTest(String v1, int b1, String v2, int b2, int expected) {
        AppVersionUtil version1 = AppVersionUtil.of(v1, b1);
        AppVersionUtil version2 = AppVersionUtil.of(v2, b2);

        int result = version1.compareTo(version2);

        if (expected > 0) assertThat(result).isPositive();
        else if (expected < 0) assertThat(result).isNegative();
        else assertThat(result).isZero();
    }

    @Test
    @DisplayName("null 입력 시 예외 발생 테스트")
    void nullInputTest() {
        assertThrows(NullPointerException.class, () -> {
            AppVersionUtil.of(null, 100);
        });
    }

    @Test
    @DisplayName("비정상적인 빌드 번호 처리 - 음수는 0으로 처리")
    void negativeBuildTest() {
        AppVersionUtil version = AppVersionUtil.of("1.0.0", -50);
        assertThat(version.build()).isZero();
    }
}