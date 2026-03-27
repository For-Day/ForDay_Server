package com.example.ForDay.domain.notification.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NotificationMessageGenerator {
    public static final String REACTION_TITLE = "내 기록에 새로운 반응!";
    private static final String REACTION_BODY_FORMAT = "%s님이 내 기록에 %s를 남겼어요.";

    public static String generateReactionBody(String nickname, String emotionName) {
        return String.format(REACTION_BODY_FORMAT, nickname, emotionName);
    }
}