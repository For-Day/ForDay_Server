package com.example.ForDay.global.firebase.dto.request;

import com.example.ForDay.global.rabbitmq.dto.NotificationEventDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FcmNotificationReqDto {
    private String fcmToken;
    private String title;
    private String body;
    private String image;
    private Map<String, String> data;

    public static FcmNotificationReqDto of(String fcmToken, NotificationEventDto eventDto) {
        return new FcmNotificationReqDto(
                fcmToken,
                eventDto.getTitle(),
                eventDto.getBody(),
                null,
                eventDto.getData()
        );
    }

}