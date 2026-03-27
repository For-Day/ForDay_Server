package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.firebase.dto.request.FcmNotificationReqDto;
import com.example.ForDay.global.firebase.service.FcmTokenService;
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
    private final FcmTokenService fcmTokenService;

    @RabbitListener(queues = RabbitMqConfig.NOTIFICATION_QUEUE)
    public void consumeRecordNotification(NotificationEventDto eventDto) {
        User receiver = eventDto.getReceiver();
        if(receiver == null) return;

        List<String> tokens = notificationService.findActiveRecordDeviceToken(receiver);

        for (String token : tokens) {
            FcmNotificationReqDto reqDto = FcmNotificationReqDto.of(token, eventDto);

            fcmTokenService.sendNotificationByToken(reqDto);
        }
    }
}