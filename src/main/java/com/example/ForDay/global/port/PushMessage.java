package com.example.ForDay.global.port;

import java.util.Map;

/**
 * 푸시 한 건. 특정 벤더(FCM) 타입에 묶이지 않는다.
 */
public record PushMessage(
        String deviceToken,
        String title,
        String body,
        Map<String, String> data
) {
}
