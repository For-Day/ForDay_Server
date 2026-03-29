package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.notification.dto.request.SendPushMessageReqDto;
import com.example.ForDay.domain.notification.dto.request.UpdatePushNotificationToggleReqDto;
import com.example.ForDay.domain.notification.dto.response.GetNotificationListResDto;
import com.example.ForDay.domain.notification.dto.response.GetPushNotificationToggleResDto;
import com.example.ForDay.domain.notification.dto.response.SendPushMessageResDto;
import com.example.ForDay.domain.notification.dto.response.UpdatePushNotificationToggleResDto;
import com.example.ForDay.domain.notification.entity.Notification;
import com.example.ForDay.domain.notification.entity.ReactionNotification;
import com.example.ForDay.domain.notification.repository.NotificationRepository;
import com.example.ForDay.domain.notification.type.NotificationFilterType;
import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.domain.notification.utils.NotificationMessageGenerator;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.firebase.entity.FcmToken;
import com.example.ForDay.global.firebase.repository.FcmTokenRepository;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.rabbitmq.config.RabbitMqConfig;
import com.example.ForDay.global.rabbitmq.dto.NotificationEventDto;
import com.example.ForDay.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final UserUtil userUtil;

    @Transactional(readOnly = true)
    public GetNotificationListResDto getNotificationList(NotificationFilterType filterType, Long lastNotificationId, Integer pageSize, User user) {
        return notificationRepository.getNotificationList(filterType, lastNotificationId, pageSize, user);
    }

    @Transactional
    public UpdatePushNotificationToggleResDto updatePushNotificationToggle(UpdatePushNotificationToggleReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        FcmToken fcmToken = fcmTokenRepository.findByUserIdAndDeviceId(currentUser.getId(), reqDto.getDeviceId())
                .orElseThrow(() -> new CustomException(ErrorCode.FCM_TOKEN_NOT_FOUND));

        switch (reqDto.getToggleType()) {
            case APP -> {
                if (isSameStatus(fcmToken.isAppPushEnabled(), reqDto.isActive())) {
                    return UpdatePushNotificationToggleResDto.alreadySameStatus(reqDto.getDeviceId(), reqDto.isActive(), reqDto.getToggleType());
                }
                fcmToken.updateAppPushEnabled(reqDto.isActive());
            }
            case RECORD -> {
                if (isSameStatus(fcmToken.isRecordPushEnabled(), reqDto.isActive())) {
                    return UpdatePushNotificationToggleResDto.alreadySameStatus(reqDto.getDeviceId(), reqDto.isActive(), reqDto.getToggleType());
                }
                fcmToken.updateRecordPushEnabled(reqDto.isActive());
            }
        }

        return UpdatePushNotificationToggleResDto.of(reqDto.getDeviceId(), reqDto.isActive(), reqDto.getToggleType());
    }

    public void processReactionNotification(User sender, User receiver, RecordReactionType reactionType, Long recordId, String imageUrl) {
        String body = NotificationMessageGenerator.generateReactionBody(sender.getNickname(), reactionType.getDescription());

        // ReactionNotification 객체 생성
        ReactionNotification savedNotification = notificationRepository.save(ReactionNotification.create(receiver, sender, NotificationType.RECORD_REACTION, body, reactionType, recordId, imageUrl));
        // 푸시 알림 로직 수행
        eventPublisher.publishEvent(NotificationEventDto.of(receiver, NotificationMessageGenerator.REACTION_TITLE, body, createDataForReaction(recordId, savedNotification.getId())));
        //sendNotificationEvent(receiver, NotificationMessageGenerator.REACTION_TITLE, body, createDataForReaction(recordId, NotificationType.RECORD_REACTION));
    }

    // rabbitMq에 메세지 이벤트 발행
    public void sendNotificationEvent(User receiver, String title, String body, Map<String, String> data) {
        NotificationEventDto eventDto = NotificationEventDto.of(receiver, title, body, data);

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.NOTIFICATION_EXCHANGE,
                RabbitMqConfig.NOTIFICATION_ROUTING_KEY,
                eventDto
        );
    }

    @Transactional(readOnly = true)
    public List<String> findActiveRecordDeviceToken(User targetUser) {
        List<FcmToken> fcmTokenList = fcmTokenRepository.findAllByUserIdAndIsRecordPushEnabledTrue(targetUser.getId());

        return fcmTokenList.stream()
                .map(FcmToken::getFcmToken)
                .toList();
    }

    @Transactional(readOnly = true)
    public GetPushNotificationToggleResDto getPushNotificationToggle(String deviceId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        FcmToken fcmToken = fcmTokenRepository.findByUserIdAndDeviceId(currentUser.getId(), deviceId).orElseThrow(() -> new CustomException(ErrorCode.FCM_TOKEN_NOT_FOUND));

        return GetPushNotificationToggleResDto.of(fcmToken.isAppPushEnabled(), fcmToken.isRecordPushEnabled());
    }

    private Map<String, String> createDataForReaction(Long recordId, Long notificationId) {
        return Map.of(
                "recordId", String.valueOf(recordId),
                "type", NotificationType.RECORD_REACTION.name(),
                "landingUrl", "/api/v2/records/" + recordId + "?notificationId=" + notificationId + "&context=USER_FEED"
        );
    }

    private boolean isSameStatus(boolean pushEnabled, boolean active) {
        return Objects.equals(pushEnabled, active);
    }

    public SendPushMessageResDto sendPushMessage(SendPushMessageReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        rabbitTemplate.convertAndSend(
                RabbitMqConfig.NOTIFICATION_EXCHANGE,
                RabbitMqConfig.NOTIFICATION_ROUTING_KEY,
                NotificationEventDto.builder()
                        .receiver(currentUser)
                        .title(reqDto.getTitle())
                        .body(reqDto.getBody())
                        .data(Map.of(
                                "landingUrl", "/api/v2/records/" + reqDto.getRecordId() + "?notificationId=" + reqDto.getNotificationId() + "&context=USER_FEED")
                        )
        );

        return new SendPushMessageResDto("성공적으로 푸시 알림이 전송되었습니다.");
    }

    public void markAsReadIfUnread(Long notificationId) {
        log.info("읽음 표시 시작");
        if(notificationId != null) {
            notificationRepository.findById(notificationId).ifPresent(Notification::markAsRead);
        }
    }
}