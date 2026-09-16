package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.notification.entity.ReactionNotification;
import com.example.ForDay.domain.notification.repository.NotificationRepository;
import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.domain.notification.utils.NotificationMessageGenerator;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.port.PushMessage;
import com.example.ForDay.global.port.PushSenderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 리액션 알림을 트랜잭션 커밋을 기다리지 않고 그 자리에서 동기 발송하는 측정 전용 경로.
 *
 * <p>정상 경로({@link NotificationService#processReactionNotification})는
 * {@code @TransactionalEventListener(AFTER_COMMIT)} → RabbitMQ를 거쳐 비동기로 발송된다.
 * 이 클래스는 그 비동기 처리가 응답 시간에 미치는 효과를 계측하기 위한 대조군이며,
 * {@code measure} 프로파일에서만 빈으로 등록된다 — {@code local}/{@code test}는 물론
 * 프로덕션 프로파일(`blue`/`green`)에도 존재하지 않는다.
 *
 * <p>{@link NotificationService#testProcessReactionNotification}이 이 클래스로 위임한다.
 */
@Slf4j
@Service
@Profile("measure")
@RequiredArgsConstructor
public class SyncPushNotificationSender {
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final PushSenderPort pushSenderPort;

    public void sendReactionNotificationSync(User sender, User receiver, RecordReactionType reactionType, Long recordId, String imageUrl) {
        String notificationContent = NotificationMessageGenerator.generateReactionContent(sender.getNickname(), reactionType.getDescription());
        String pushReactionBody = NotificationMessageGenerator.generatePushReactionBody(receiver.getNickname(), reactionType.getDescription());

        ReactionNotification savedNotification =
                notificationRepository.save(
                        ReactionNotification.create(receiver, sender, NotificationType.RECORD_REACTION, notificationContent, reactionType, recordId, imageUrl)
                );

        List<String> tokens = notificationService.findActiveRecordDeviceToken(receiver);

        if (tokens.isEmpty()) {
            return;
        }

        log.info("[FCM-Sync] 동기 발송 시작 - 유저 ID: {}, 토큰 개수: {}개", receiver.getId(), tokens.size());

        Map<String, String> data = NotificationMessageGenerator.createDataForReaction(recordId, savedNotification.getId());

        for (String token : tokens) {
            try {
                pushSenderPort.send(new PushMessage(
                        token, sender.getNickname(), pushReactionBody, data));
                log.info("[FCM-Sync] 동기 전송 성공 - Token: {}", token);
            } catch (Exception e) {
                log.error("[FCM-Sync] 동기 전송 중 에러 발생 - Token: {}, Error: {}", token, e.getMessage());
            }
        }
    }
}
