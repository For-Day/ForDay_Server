package com.example.ForDay.global.port;

import java.util.Map;

/**
 * 푸시 한 건. 특정 벤더(FCM) 타입에 묶이지 않는다.
 *
 * <p>{@code notificationId}는 "같은 알림인가"를 판정하는 유일한 근거다. 이전에는 이 값이
 * {@code data}의 landingUrl 쿼리 파라미터 안에만 묻혀 있어서, 중복 발송을 막으려 해도
 * 문자열을 파싱하지 않고는 동일성을 알 수 없었다.
 */
public record PushMessage(
        Long notificationId,
        String deviceToken,
        String title,
        String body,
        Map<String, String> data
) {
}
