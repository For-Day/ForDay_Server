package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.notification.dto.request.SendPushMessageReqDto;
import com.example.ForDay.domain.notification.dto.request.UpdatePushNotificationToggleReqDto;
import com.example.ForDay.domain.notification.dto.response.GetNotificationListResDto;
import com.example.ForDay.domain.notification.dto.response.GetPushNotificationToggleResDto;
import com.example.ForDay.domain.notification.dto.response.SendPushMessageResDto;
import com.example.ForDay.domain.notification.dto.response.UpdatePushNotificationToggleResDto;
import com.example.ForDay.domain.notification.entity.Notification;
import com.example.ForDay.domain.notification.entity.NotificationOutbox;
import com.example.ForDay.domain.notification.entity.ReactionNotification;
import com.example.ForDay.domain.notification.repository.NotificationOutboxRepository;
import com.example.ForDay.domain.notification.repository.NotificationRepository;
import com.example.ForDay.domain.notification.type.NotificationFilterType;
import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.domain.notification.utils.NotificationMessageGenerator;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.common.response.message.NotificationSuccessCode;
import com.example.ForDay.global.port.PushMessage;
import com.example.ForDay.global.port.PushSenderPort;
import com.example.ForDay.global.firebase.entity.FcmToken;
import com.example.ForDay.global.firebase.repository.FcmTokenRepository;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.rabbitmq.dto.NotificationEventDto;
import com.example.ForDay.global.util.UserUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationOutboxRepository notificationOutboxRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final ObjectMapper objectMapper;
    private final UserUtil userUtil;
    private final UserRepository userRepository;
    private final PushSenderPort pushSenderPort;
    // measure 프로파일에서만 빈으로 존재한다. 생성자 주입 대신 ObjectProvider로 받아
    // NotificationService 조립 시점에 SyncPushNotificationSender가 없어도(local/test/prod)
    // 실패하지 않게 한다. SyncPushNotificationSender가 이 서비스를 다시 참조하므로
    // 즉시 주입이었다면 순환 의존이 됐을 것이다.
    private final ObjectProvider<SyncPushNotificationSender> syncPushNotificationSenderProvider;

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

    /**
     * 알림 저장과 "발행해야 한다"는 사실을 같은 트랜잭션 안에서 원자적으로 커밋한다
     * (Outbox 패턴). 예전에는 저장 후 {@code AFTER_COMMIT} 이벤트로 RabbitMQ를 직접
     * 호출했는데, 그 방식은 (1) 발행이 실패하면 재시도 없이 유실되고 (2) 발행 중 예외가
     * 트랜잭션 밖으로 전파돼 DB에는 이미 커밋된 리액션이 클라이언트에는 500으로 보이는
     * 문제가 있었다. 실제 발행은 {@link NotificationOutboxRelay}가 별도로 맡는다.
     * 배경: {@code docs/adr/0002-notification-publish-after-commit.md}
     */
    public void processReactionNotification(User sender, User receiver, RecordReactionType reactionType, Long recordId, String imageUrl) {
        String notificationContent = NotificationMessageGenerator.generateReactionContent(sender.getNickname(), reactionType.getDescription()); // notification 내용
        String pushReactionBody = NotificationMessageGenerator.generatePushReactionBody(receiver.getNickname(), reactionType.getDescription()); // 푸시 알림 body 내용

        ReactionNotification savedNotification =
                notificationRepository.save(
                        ReactionNotification.create(receiver, sender, NotificationType.RECORD_REACTION, notificationContent, reactionType, recordId, imageUrl)
                );

        List<String> tokens = findActiveRecordDeviceToken(receiver);

        if (!tokens.isEmpty()) {
            NotificationEventDto event = NotificationEventDto.of(
                    receiver,
                    tokens,
                    sender.getNickname(),
                    pushReactionBody,
                    NotificationMessageGenerator.createDataForReaction(recordId, savedNotification.getId())
            );
            notificationOutboxRepository.save(NotificationOutbox.pending(savedNotification.getId(), toJson(event)));
        }
    }

    private String toJson(NotificationEventDto event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            // NotificationEventDto는 순수 필드(문자열·리스트·맵)만 갖고 있어 직렬화가 실패할
            // 구조적 이유가 없다 - 발생하면 설정 오류에 가까우므로 즉시 드러나는 게 낫다.
            throw new IllegalStateException("알림 이벤트 직렬화 실패", e);
        }
    }

    /**
     * 동기(이 메서드) vs 비동기({@link #processReactionNotification}) 응답 시간을 비교 측정하기
     * 위한 전용 경로다. 실제 발송 로직은 {@code measure} 프로파일에서만 등록되는
     * {@link SyncPushNotificationSender}에 있다 — 삭제하지 말 것. #370이 보류한 측정
     * 항목(동기/비동기 응답시간 재측정)이 이 경로를 사용한다.
     *
     * <p>{@code measure} 프로파일이 꺼져 있으면(local/test/prod 전부 해당) 호출할 방법 자체가
     * 없다 — 이 메서드를 호출하는 {@code TestReactionMeasurementController}도 같은 프로파일로
     * 게이트돼 있기 때문이다. 그래도 이 메서드가 프로그램적으로 직접 호출되는 경우를 대비해
     * 방어적으로 예외를 던진다.
     */
    public void testProcessReactionNotification(User sender, User receiver, RecordReactionType reactionType, Long recordId, String imageUrl) {
        SyncPushNotificationSender syncSender = syncPushNotificationSenderProvider.getIfAvailable();
        if (syncSender == null) {
            throw new IllegalStateException(
                    "SyncPushNotificationSender는 'measure' 프로파일에서만 등록된다. " +
                            "--spring.profiles.active에 measure를 포함해 실행했는지 확인할 것.");
        }
        syncSender.sendReactionNotificationSync(sender, receiver, reactionType, recordId, imageUrl);
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

    /**
     * {@code TestNotificationController} 전용 디버그 경로다. 프로덕션 알림 발송 경로가
     * 아니다 — 실제 발송은 {@link #processReactionNotification}이 남긴 outbox 행을
     * {@link NotificationOutboxRelay}가 읽어 발행하는 경로 하나뿐이다. 여기는 DB 접근이
     * 없어 {@code @Transactional}이 불필요했고(커넥션만 붙잡은 채 FCM 호출을 기다렸다), 제거했다.
     */
    public SendPushMessageResDto sendPushMessage(SendPushMessageReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        NotificationEventDto eventDto = NotificationEventDto.of(
                currentUser,
                List.of(reqDto.getFcmToken()),
                NotificationMessageGenerator.REACTION_TITLE,
                reqDto.getBody(),
                NotificationMessageGenerator.createDataForReaction(reqDto.getRecordId(), reqDto.getNotificationId())
        );

        pushSenderPort.send(new PushMessage(
                reqDto.getFcmToken(), eventDto.getTitle(), eventDto.getBody(), eventDto.getData()));

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
}