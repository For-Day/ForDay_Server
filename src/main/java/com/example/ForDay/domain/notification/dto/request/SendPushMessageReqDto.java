package com.example.ForDay.domain.notification.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendPushMessageReqDto {
    private String title;
    private String body;
    private Long recordId;
    private Long notificationId;
    private String fcmToken;
}