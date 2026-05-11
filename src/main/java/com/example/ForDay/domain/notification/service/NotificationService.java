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
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.common.response.message.NotificationSuccessCode;
import com.example.ForDay.global.firebase.dto.request.FcmNotificationReqDto;
import com.example.ForDay.global.firebase.entity.FcmToken;
import com.example.ForDay.global.firebase.repository.FcmTokenRepository;
import com.example.ForDay.global.firebase.service.FcmTokenService;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.rabbitmq.dto.NotificationEventDto;
import com.example.ForDay.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    public static final String RECORD_DETAIL_URL = "/api/v2/records/";
    private final NotificationRepository notificationRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final UserUtil userUtil;
    private final UserRepository userRepository;
    private final FcmTokenService fcmTokenService;

    @Transactional(readOnly = true)
    public GetNotificationListResDto getNotificationList(NotificationFilterType filterType, Long lastNotificationId, Integer pageSize, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        if (!currentUser.isRecordPushEnabled()) {
            return GetNotificationListResDto.notPushEnabled();
        }
        return notificationRepository.getNotificationList(filterType, lastNotificationId, pageSize, currentUser);
    }

    @Transactional
    public UpdatePushNotificationToggleResDto updatePushNotificationToggle(UpdatePushNotificationToggleReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        switch (reqDto.getToggleType()) {
            case APP -> {
                if (isSameStatus(currentUser.isAppPushEnabled(), reqDto.isActive())) {
                    return UpdatePushNotificationToggleResDto.alreadySameStatus(reqDto.isActive(), reqDto.getToggleType());
                }
                currentUser.updateAppPushEnabled(reqDto.isActive());
            }
            case RECORD -> {
                if (isSameStatus(currentUser.isRecordPushEnabled(), reqDto.isActive())) {
                    return UpdatePushNotificationToggleResDto.alreadySameStatus(reqDto.isActive(), reqDto.getToggleType());
                }
                currentUser.updateRecordPushEnabled(reqDto.isActive());
            }
        }
        userRepository.save(currentUser);
        return UpdatePushNotificationToggleResDto.of(reqDto.isActive(), reqDto.getToggleType());
    }

    public void processReactionNotification(User sender, User receiver, RecordReactionType reactionType, Long recordId, String imageUrl) {
        String notificationContent = NotificationMessageGenerator.generateReactionContent(sender.getNickname(), reactionType.getDescription()); // notification 내용
        String pushReactionBody = NotificationMessageGenerator.generatePushReactionBody(receiver.getNickname(), reactionType.getDescription()); // 푸시 알림 body 내용

        ReactionNotification savedNotification =
                notificationRepository.save(
                        ReactionNotification.create(receiver, sender, NotificationType.RECORD_REACTION, notificationContent, reactionType, recordId, imageUrl)
                );

        List<String> tokens = findActiveRecordDeviceToken(receiver);

        if (!tokens.isEmpty()) {
            eventPublisher.publishEvent(NotificationEventDto.of(
                    receiver,
                    tokens,
                    sender.getNickname(),
                    pushReactionBody,
                    createDataForReaction(recordId, savedNotification.getId())
            ));
        }
    }

    public void testProcessReactionNotification(User sender, User receiver, RecordReactionType reactionType, Long recordId, String imageUrl) {
        String notificationContent = NotificationMessageGenerator.generateReactionContent(sender.getNickname(), reactionType.getDescription());
        String pushReactionBody = NotificationMessageGenerator.generatePushReactionBody(receiver.getNickname(), reactionType.getDescription());

        ReactionNotification savedNotification =
                notificationRepository.save(
                        ReactionNotification.create(receiver, sender, NotificationType.RECORD_REACTION, notificationContent, reactionType, recordId, imageUrl)
                );

        List<String> tokens = findActiveRecordDeviceToken(receiver);

        if (!tokens.isEmpty()) {
            log.info("[FCM-Sync] 동기 발송 시작 - 유저 ID: {}, 토큰 개수: {}개", receiver.getId(), tokens.size());

            Map<String, String> data = createDataForReaction(recordId, savedNotification.getId());

            for (String token : tokens) {
                try {
                    FcmNotificationReqDto reqDto = new FcmNotificationReqDto(
                            token,
                            sender.getNickname(),
                            pushReactionBody,
                            null,
                            data
                    );

                    fcmTokenService.sendNotificationByToken(reqDto);
                    log.info("[FCM-Sync] 동기 전송 성공 - Token: {}", token);
                } catch (Exception e) {
                    log.error("[FCM-Sync] 동기 전송 중 에러 발생 - Token: {}, Error: {}", token, e.getMessage());
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public List<String> findActiveRecordDeviceToken(User targetUser) {
        if (!targetUser.isRecordPushEnabled()) {
            log.info("유저의 알림이 활성화되어 있지 않습니다.");
            return Collections.emptyList();
        }

        List<FcmToken> fcmTokenList = fcmTokenRepository.findByUserId(targetUser.getId());
        log.info("유저 fcm 조회 완료 {}", fcmTokenList);
        return fcmTokenList.stream()
                .map(FcmToken::getFcmToken)
                .toList();
    }

    @Transactional(readOnly = true)
    public GetPushNotificationToggleResDto getPushNotificationToggle(CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        return GetPushNotificationToggleResDto.of(currentUser.isAppPushEnabled(), currentUser.isRecordPushEnabled());
    }

    @Transactional
    public SendPushMessageResDto sendPushMessage(SendPushMessageReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        NotificationEventDto eventDto = NotificationEventDto.of(
                currentUser,
                List.of(reqDto.getFcmToken()),
                NotificationMessageGenerator.REACTION_TITLE,
                reqDto.getBody(),
                createDataForReaction(reqDto.getRecordId(), reqDto.getNotificationId())
        );

        FcmNotificationReqDto fcmSendReqDto = FcmNotificationReqDto.of(
                reqDto.getFcmToken(),
                eventDto
        );

        fcmTokenService.sendNotificationByToken(fcmSendReqDto);

        return new SendPushMessageResDto(NotificationSuccessCode.SEND_NOTIFICATION_SUCCESS.getMessage());
    }

    private boolean isSameStatus(boolean pushEnabled, boolean active) {
        return Objects.equals(pushEnabled, active);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markAsReadIfUnread(Long notificationId) {
        log.info("읽음 표시 시작");
        if (notificationId != null) {
            notificationRepository.findById(notificationId).ifPresent(Notification::markAsRead);
        }
    }

    public boolean unreadNotificationExists(User user) {
        return notificationRepository.existsByReceiverIdAndIsReadFalse(user.getId());
    }

    private Map<String, String> createDataForReaction(Long recordId, Long notificationId) {
        return Map.of(
                "recordId", String.valueOf(recordId),
                "type", NotificationType.RECORD_REACTION.name(),
                "landingUrl", RECORD_DETAIL_URL + recordId + "?notificationId=" + notificationId + "&context=USER_FEED",
                "sendAt", LocalDateTime.now().toString()
        );
    }
}