package com.example.ForDay.global.rabbitmq.dto;

import com.example.ForDay.domain.user.entity.User;
import lombok.*;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEventDto {
    private User receiver;
    private String title;
    private String body;
    private Map<String, String> data; // 클릭 시 이동할 페이지 정보 등

    public static NotificationEventDto of(User receiver, String title, String body, Map<String, String> data) {
        return new NotificationEventDto(
                receiver,
                title,
                body,
                data
        );
    }
}