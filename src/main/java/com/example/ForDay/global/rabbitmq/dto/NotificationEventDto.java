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
    private String receiverId;
    private List<String> fcmTokens;
    private String title;
    private String body;
    private Map<String, String> data;

    public static NotificationEventDto of(User receiver, List<String> tokens, String title, String body, Map<String, String> data) {
        return NotificationEventDto.builder()
                .receiverId(receiver.getId())
                .fcmTokens(tokens)
                .title(title)
                .body(body)
                .data(data)
                .build();
    }
}