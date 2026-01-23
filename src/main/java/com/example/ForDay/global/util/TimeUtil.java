package com.example.ForDay.global.util;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TimeUtil {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    /**
     * LocalDateTime을 "yyyy.MM.dd HH:mm" 문자열 형식으로 변환합니다.
     */
    public static String formatLocalDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(FORMATTER);
    }
}
