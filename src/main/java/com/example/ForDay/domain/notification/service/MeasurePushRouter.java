package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * 측정 경로를 1단계(동기)와 2단계({@code @Async}) 중 하나로 고르는 전환 스위치.
 * {@code measure} 프로파일 전용.
 *
 * <p>엔드포인트와 k6 스크립트를 그대로 두고 설정만 바꿔 전환한다. 그래야 두 단계의 차이가
 * "발송을 요청 스레드에서 했는가" 하나로 좁혀지고, 응답 시간 차이를 그 원인으로 돌릴 수 있다.
 *
 * <p>{@link NotificationService}에 모드 분기를 두지 않고 이 클래스로 뺀 이유 — 그렇게 하면
 * 주입 의존성이 9개가 되어 ArchUnit S1(서비스의 주입 의존성 8개 이하)을 위반한다. 애초에
 * 알림 서비스가 "지금 몇 단계를 측정 중인지"를 알 필요도 없다.
 */
@Service
@Profile("measure")
@RequiredArgsConstructor
public class MeasurePushRouter {

    private static final String ASYNC_MODE = "async";

    private final SyncPushNotificationSender syncSender;
    private final AsyncPushNotificationSender asyncSender;

    /** {@code sync} 또는 {@code async}. 실행 중 덮어쓰려면 {@code --measure.push.mode=async}. */
    @Value("${measure.push.mode:sync}")
    private String mode;

    public void sendReactionNotification(User sender, User receiver, RecordReactionType reactionType, Long recordId, String imageUrl) {
        if (ASYNC_MODE.equalsIgnoreCase(mode)) {
            asyncSender.sendReactionNotificationAsync(sender, receiver, reactionType, recordId, imageUrl);
            return;
        }
        syncSender.sendReactionNotificationSync(sender, receiver, reactionType, recordId, imageUrl);
    }

    public String currentMode() {
        return ASYNC_MODE.equalsIgnoreCase(mode) ? ASYNC_MODE : "sync";
    }
}
