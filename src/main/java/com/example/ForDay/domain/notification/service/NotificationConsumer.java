package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.firebase.dto.request.FcmNotificationReqDto;
import com.example.ForDay.global.firebase.service.FcmTokenService;
import com.example.ForDay.global.rabbitmq.config.RabbitMqConfig;
import com.example.ForDay.global.rabbitmq.dto.NotificationEventDto;
import jakarta.persistence.EntityNotFoundException;
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
    private final FcmTokenService fcmTokenService;
    private final UserRepository userRepository;

    @RabbitListener(queues = RabbitMqConfig.NOTIFICATION_QUEUE)
    public void consumeRecordNotification(NotificationEventDto eventDto) {
        log.info("[RabbitMQ] 메시지 수신 성공: {}", eventDto);
        User receiver = eventDto.getReceiver();

        //String userId = eventDto.getReceiver().getId();

        // DB에서 최신 유저 정보를 다시 조회 (중요!)
        //User receiver = userRepository.findById(userId)
        //        .orElseThrow(() -> new EntityNotFoundException("유저를 찾을 수 없습니다."));

        if (receiver == null) {
            log.warn("[RabbitMQ] 수신자(Receiver) 정보가 없어 처리를 중단합니다.");
            return;
        }

        List<String> tokens = notificationService.findActiveRecordDeviceToken(receiver);

        if (tokens.isEmpty()) {
            log.warn("[FCM] 유저({})에게 전송할 활성화된 토큰이 없습니다.", receiver.getId());
            return;
        }

        log.info("[FCM] 유저({})에게 발송 시작 - 토큰 개수: {}개", receiver.getId(), tokens.size());

        for (String token : tokens) {
            try {
                FcmNotificationReqDto reqDto = FcmNotificationReqDto.of(token, eventDto);
                fcmTokenService.sendNotificationByToken(reqDto);
                log.info("[FCM] 전송 요청 완료 - Token: {}", token);
            } catch (Exception e) {
                log.error("[FCM] 전송 중 에러 발생 - Token: {}, Error: {}", token, e.getMessage());
            }
        }
    }
}