package com.example.ForDay.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TimeUtilTest {

    @Test
    @DisplayName("초 단위 전 시간 포맷 확인")
    void formatTimeAgo_Seconds() {
        // given: 현재 시간보다 30초 전
        LocalDateTime createdAt = LocalDateTime.now().minusSeconds(30);

        // when
        String result = TimeUtil.formatTimeAgo(createdAt);

        // then
        assertThat(result).isEqualTo("30초 전");
    }

    @Test
    @DisplayName("분 단위 전 시간 포맷 확인")
    void formatTimeAgo_Minutes() {
        // given: 현재 시간보다 5분 전
        LocalDateTime createdAt = LocalDateTime.now().minusMinutes(5);

        // when
        String result = TimeUtil.formatTimeAgo(createdAt);

        // then
        assertThat(result).isEqualTo("5분 전");
    }

    @Test
    @DisplayName("시간 단위 전 시간 포맷 확인")
    void formatTimeAgo_Hours() {
        // given: 현재 시간보다 3시간 전
        LocalDateTime createdAt = LocalDateTime.now().minusHours(3);

        // when
        String result = TimeUtil.formatTimeAgo(createdAt);

        // then
        assertThat(result).isEqualTo("3시간 전");
    }

    @Test
    @DisplayName("어제 날짜 포맷 확인")
    void formatTimeAgo_Yesterday() {
        // given: 현재 시간보다 1일 전 (24시간 이상 48시간 미만)
        LocalDateTime createdAt = LocalDateTime.now().minusHours(26);

        // when
        String result = TimeUtil.formatTimeAgo(createdAt);

        // then
        assertThat(result).isEqualTo("어제");
    }

    @Test
    @DisplayName("일 단위 전 시간 포맷 확인 (7일 미만)")
    void formatTimeAgo_Days() {
        // given: 현재 시간보다 5일 전
        LocalDateTime createdAt = LocalDateTime.now().minusDays(5);

        // when
        String result = TimeUtil.formatTimeAgo(createdAt);

        // then
        assertThat(result).isEqualTo("5일 전");
    }

    @Test
    @DisplayName("7일 이상 경과 시 날짜 포맷 확인")
    void formatTimeAgo_FullDate() {
        // given: 특정 과거 날짜
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);

        // when
        String result = TimeUtil.formatTimeAgo(createdAt);

        // then
        assertThat(result).isEqualTo("2026-01-01");
    }
}