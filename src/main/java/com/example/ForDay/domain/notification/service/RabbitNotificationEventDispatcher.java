package com.example.ForDay.domain.notification.service;

import com.example.ForDay.global.rabbitmq.config.RabbitMqConfig;
import com.example.ForDay.global.rabbitmq.dto.NotificationEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** 운영 기본 발행 경로 — outbox 행을 RabbitMQ로 내보낸다. */
@Component
@RequiredArgsConstructor
public class RabbitNotificationEventDispatcher implements NotificationEventDispatcher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void dispatch(NotificationEventDto event) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.NOTIFICATION_EXCHANGE, RabbitMqConfig.NOTIFICATION_ROUTING_KEY, event);
    }
}
