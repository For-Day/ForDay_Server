package com.example.ForDay.global.util;

import org.springframework.stereotype.Component;

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
}