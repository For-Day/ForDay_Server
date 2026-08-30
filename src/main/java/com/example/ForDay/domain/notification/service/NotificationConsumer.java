package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.port.PushMessage;
import com.example.ForDay.global.port.PushSenderPort;
import com.example.ForDay.global.rabbitmq.config.RabbitMqConfig;
import com.example.ForDay.global.rabbitmq.dto.NotificationEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationConsumer {
    private final NotificationService notificationService;
    private final PushSenderPort pushSenderPort;
    private final UserRepository userRepository;

    @RabbitListener(queues = RabbitMqConfig.NOTIFICATION_QUEUE)
    public void consumeRecordNotification(NotificationEventDto eventDto) {
        log.info("[RabbitMQ] 메시지 수신 - ReceiverId: {}, Title: {}", eventDto.getReceiverId(), eventDto.getTitle());

        List<String> tokens = eventDto.getFcmTokens();

        if (tokens == null || tokens.isEmpty()) {
            log.warn("[RabbitMQ] 전송할 FCM 토큰이 없어 처리를 중단합니다. ReceiverId: {}", eventDto.getReceiverId());
            return;
        }

        log.info("[FCM] 발송 시작 - 유저 ID: {}, 토큰 개수: {}개", eventDto.getReceiverId(), tokens.size());

        for (String token : tokens) {
            try {
                pushSenderPort.send(new PushMessage(
                        token, eventDto.getTitle(), eventDto.getBody(), eventDto.getData()));
                log.info("[FCM] 전송 요청 성공 - Token: {}", token);
            } catch (Exception e) {
                // 특정 토큰 전송 실패 시 로그 남기고 다음 토큰으로 진행
                log.error("[FCM] 전송 중 에러 발생 - Token: {}, Error: {}", token, e.getMessage());
            }
        }
    }
}