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
    private final NotificationDeduplicator deduplicator;

    @RabbitListener(queues = RabbitMqConfig.NOTIFICATION_QUEUE)
    public void consumeRecordNotification(NotificationEventDto eventDto) {
        log.info("[RabbitMQ] 메시지 수신 - ReceiverId: {}, Title: {}", eventDto.getReceiverId(), eventDto.getTitle());

        List<String> tokens = eventDto.getFcmTokens();

        if (tokens == null || tokens.isEmpty()) {
            log.warn("[RabbitMQ] 전송할 FCM 토큰이 없어 처리를 중단합니다. ReceiverId: {}", eventDto.getReceiverId());
            return;
        }

        // 아웃박스와 브로커는 둘 다 at-least-once다 - 같은 알림이 두 번 도착하는 것은 정상
        // 동작이며, 딱 한 번 보내는 책임은 받는 쪽에 있다.
        if (!deduplicator.claim(eventDto.getNotificationId())) {
            log.info("[RabbitMQ] 이미 발송된 알림이라 건너뜁니다 - notificationId: {}", eventDto.getNotificationId());
            return;
        }

        log.info("[FCM] 발송 시작 - 유저 ID: {}, 토큰 개수: {}개", eventDto.getReceiverId(), tokens.size());

        try {
            for (String token : tokens) {
                try {
                    pushSenderPort.send(new PushMessage(
                            eventDto.getNotificationId(), token, eventDto.getTitle(), eventDto.getBody(), eventDto.getData()));
                    log.info("[FCM] 전송 요청 성공 - Token: {}", token);
                } catch (Exception e) {
                    // 특정 토큰 전송 실패 시 로그 남기고 다음 토큰으로 진행
                    log.error("[FCM] 전송 중 에러 발생 - Token: {}, Error: {}", token, e.getMessage());
                }
            }
        } catch (Exception e) {
            // 선점만 해두고 발송을 못 하면 그 알림은 영영 재시도되지 않는다. 되돌린다.
            deduplicator.release(eventDto.getNotificationId());
            throw e;
        }
    }
}