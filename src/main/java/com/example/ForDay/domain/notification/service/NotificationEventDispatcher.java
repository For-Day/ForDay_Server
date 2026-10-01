package com.example.ForDay.domain.notification.service;

import com.example.ForDay.global.rabbitmq.dto.NotificationEventDto;

/**
 * outbox 행 하나를 실제로 내보내는 동작.
 *
 * <p>{@link NotificationOutboxItemPublisher}에서 분리한 이유 — 아웃박스의 보장(커밋된 것은
 * 반드시 언젠가 나간다)과 "무엇으로 내보내는가"는 별개의 관심사다. 측정에서는 브로커 없이
 * 릴레이가 직접 발송하는 구성과 RabbitMQ를 거치는 구성을 같은 아웃박스 위에서 바꿔 끼워
 * 비교한다.
 *
 * <p>예외를 던지면 호출자가 그 행을 PENDING으로 남겨 다음 주기에 재시도한다.
 */
public interface NotificationEventDispatcher {
    void dispatch(NotificationEventDto event);
}
