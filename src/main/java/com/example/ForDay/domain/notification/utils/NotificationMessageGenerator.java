package com.example.ForDay.domain.notification.utils;

import com.example.ForDay.domain.notification.type.NotificationType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationMessageGenerator {
    public static final String REACTION_TITLE = "내 기록에 새로운 반응!";
    public static final String RECORD_DETAIL_URL = "/api/v2/records/";
    private static final String REACTION_BODY_FORMAT = "%s님이 내 기록에 %s를 남겼어요.";
    private static final String PUSH_REACTION_TITLE = "%s님이 올린 기록에 %s를 남겼어요.";

    public static String generateReactionContent(String nickname, String emotionName) {
        return String.format(REACTION_BODY_FORMAT, nickname, emotionName);
    }

    public static String generatePushReactionBody(String senderNickname, String emotionName) {
        return String.format(PUSH_REACTION_TITLE, senderNickname, emotionName);
    }

    // NotificationService(정상 경로)와 SyncPushNotificationSender(measure 프로파일 측정 경로)가
    // 같은 형식의 FCM data 페이로드를 만들어야 해서 공유 지점으로 뺐다.
    public static Map<String, String> createDataForReaction(Long recordId, Long notificationId) {
        return Map.of(
                "recordId", String.valueOf(recordId),
                "type", NotificationType.RECORD_REACTION.name(),
                "landingUrl", RECORD_DETAIL_URL + recordId + "?notificationId=" + notificationId + "&context=USER_FEED",
                "sendAt", LocalDateTime.now().toString()
        );
    }
}