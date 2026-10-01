package com.example.ForDay.global.rabbitmq.dto;

import com.example.ForDay.domain.user.entity.User;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEventDto {
    /**
     * 메시지 동일성의 근거. 브로커는 at-least-once라서 같은 메시지가 두 번 배달될 수 있고,
     * 컨슈머가 중복을 걸러내려면 "이전에 처리한 그 알림인가"를 판정할 키가 필요하다.
     * 이 필드가 없을 때는 outbox 행에만 notificationId가 있고 메시지에는 없어서,
     * 컨슈머 쪽에서 중복을 판정할 방법이 아예 없었다.
     */
    private Long notificationId;
    private String receiverId;
    private List<String> fcmTokens;
    private String title;
    private String body;
    private Map<String, String> data;

    public static NotificationEventDto of(Long notificationId, User receiver, List<String> tokens, String title, String body, Map<String, String> data) {
        return NotificationEventDto.builder()
                .notificationId(notificationId)
                .receiverId(receiver.getId())
                .fcmTokens(tokens)
                .title(title)
                .body(body)
                .data(data)
                .build();
    }
}