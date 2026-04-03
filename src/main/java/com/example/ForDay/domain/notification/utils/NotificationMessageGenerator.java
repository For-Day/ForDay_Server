package com.example.ForDay.domain.notification.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationMessageGenerator {
    public static final String REACTION_TITLE = "내 기록에 새로운 반응!";
    private static final String REACTION_BODY_FORMAT = "%s님이 내 기록에 %s를 남겼어요.";
    private static final String PUSH_REACTION_TITLE = "%s님이 올린 기록에 %s를 남겼어요.";

    public static String generateReactionContent(String nickname, String emotionName) {
        return String.format(REACTION_BODY_FORMAT, nickname, emotionName);
    }

    public static String generatePushReactionBody(String senderNickname, String emotionName) {
        return String.format(PUSH_REACTION_TITLE, senderNickname, emotionName);
    }
}