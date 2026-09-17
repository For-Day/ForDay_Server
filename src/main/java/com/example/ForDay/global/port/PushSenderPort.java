package com.example.ForDay.global.port;

/**
 * 기기로 푸시를 보낸다.
 *
 * <p>여러 도메인이 공유하므로 global에 둔다 (ADR-0001 §7 포트 위치 규칙).
 */
public interface PushSenderPort {

    /**
     * 전송 실패는 예외로 올리지 않고 삼킨다. 알림 실패가 본 작업을 되돌리면 안 되기 때문이며,
     * 기존 FcmTokenService 동작을 그대로 옮긴 것이다.
     */
    void send(PushMessage message);
}
