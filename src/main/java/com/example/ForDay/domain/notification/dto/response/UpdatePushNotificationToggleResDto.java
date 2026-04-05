package com.example.ForDay.domain.notification.dto.response;

import com.example.ForDay.domain.notification.type.ToggleType;
import com.example.ForDay.global.common.response.message.NotificationSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.NotificationSuccessCode.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePushNotificationToggleResDto {
    private String message;
    private boolean active;
    private ToggleType toggleType;

    public static UpdatePushNotificationToggleResDto of(boolean active, ToggleType toggleType) {
        return new UpdatePushNotificationToggleResDto(
                active ? NotificationSuccessCode.ACTIVE_NOTIFICATION_SUCCESS.getMessage(): NotificationSuccessCode.INACTIVE_NOTIFICATION_SUCCESS.getMessage(),
                active,
                toggleType
        );
    }

    public static UpdatePushNotificationToggleResDto alreadySameStatus(boolean active, ToggleType toggleType) {
        return new UpdatePushNotificationToggleResDto(
                NotificationSuccessCode.ALREADY_SAME_STATUS.getMessage(),
                active,
                toggleType
        );
    }
}