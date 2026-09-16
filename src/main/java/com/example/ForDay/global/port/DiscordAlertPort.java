package com.example.ForDay.global.port;

/**
 * 운영자에게 Discord로 알린다.
 *
 * <p>메시지 조립은 호출부 책임이다 - 이 포트는 "무엇을 보낼지"를 모르고 "보낸다"만 안다.
 * 그래야 알림 발행 실패 알림 외의 다른 용도(예: 배치 실패 알림)로도 재사용할 수 있다.
 */
public interface DiscordAlertPort {

    /**
     * 전송 실패는 예외로 올리지 않고 삼킨다({@code PushSenderPort} 컨벤션과 동일). 운영 알림이
     * 안 나간다고 본 작업(알림 발행 재시도 등)이 막히면 안 되기 때문이다.
     */
    void send(String message);
}
