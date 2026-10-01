package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.notification.entity.ReactionNotification;
import com.example.ForDay.domain.notification.repository.NotificationRepository;
import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.domain.notification.utils.NotificationMessageGenerator;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.measure.MeasurementRecorder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 2단계 측정 경로 — FCM 발송을 {@code @Async}로 요청 스레드에서 떼어낸다.
 *
 * <p>{@link SyncPushNotificationSender}(1단계)와 DB 작업은 동일하고, 마지막 발송 루프만
 * {@link AsyncPushDispatcher}로 넘긴다. 두 단계의 차이를 "발송이 요청 스레드 안에 있는가"
 * 하나로 좁혀야 응답 시간 비교가 그 차이 때문이라고 말할 수 있다.
 *
 * <p>발송 확정 카운터를 <b>디스패치 직전(요청 스레드)</b>에서 올린다. 비동기 메서드 안에서
 * 올리면 큐에 쌓인 채 프로세스가 죽은 작업은 애초에 집계되지 않아 유실이 0으로 보인다 —
 * 유실을 재려면 분모가 "접수한 건수"여야 한다.
 */
@Slf4j
@Service
@Profile("measure")
@RequiredArgsConstructor
public class AsyncPushNotificationSender {

    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final AsyncPushDispatcher asyncPushDispatcher;
    private final MeasurementRecorder measurementRecorder;

    public void sendReactionNotificationAsync(User sender, User receiver, RecordReactionType reactionType, Long recordId, String imageUrl) {
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

        measurementRecorder.recordAccepted();

        Map<String, String> data = NotificationMessageGenerator.createDataForReaction(recordId, savedNotification.getId());
        asyncPushDispatcher.dispatch(savedNotification.getId(), tokens, sender.getNickname(), pushReactionBody, data);
    }
}
