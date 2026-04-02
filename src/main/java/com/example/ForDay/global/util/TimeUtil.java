package com.example.ForDay.global.util;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
public class TimeUtil {
    // 하이픈(-) 사용 및 오전/오후(a) 패턴 추가 (예: 2026-01-23 오후 04:51)
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd a hh:mm", Locale.KOREAN);

    /**
     * LocalDateTime을 "yyyy-MM-dd 오전/오후 hh:mm" 문자열 형식으로 변환합니다.
     */
    public static String formatLocalDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(FORMATTER);
    }

    public static String formatTimeAgo(LocalDateTime createdAt) {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(createdAt, now);

        long seconds = duration.getSeconds();
        long minutes = duration.toMinutes();
        long hours = duration.toHours();
        long days = duration.toDays();

        if (seconds < 60) {
            return seconds + "초 전";
        } else if (minutes < 60) {
            return minutes + "분 전";
        } else if (hours < 24) {
            return hours + "시간 전";
        } else if (days == 1) {
            return "어제";
        } else if (days < 7) {
            return days + "일 전";
        } else {
            return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        }
    }

}