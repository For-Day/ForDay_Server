package com.example.ForDay.domain.notification.dto.response;

import com.example.ForDay.domain.notification.type.ToggleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.NotificationSuccessMessage.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePushNotificationToggleResDto {
    private String message;
    private boolean active;
    private String deviceId;
    private ToggleType toggleType;

    public static UpdatePushNotificationToggleResDto of(String deviceId, boolean active, ToggleType toggleType) {
        return new UpdatePushNotificationToggleResDto(
                active ? ACTIVE_NOTIFICATION_SUCCESS: INACTIVE_NOTIFICATION_SUCCESS,
                active,
                deviceId,
                toggleType
        );
    }

    public static UpdatePushNotificationToggleResDto alreadySameStatus(String deviceId, boolean active, ToggleType toggleType) {
        return new UpdatePushNotificationToggleResDto(
                ALREADY_SAME_STATUS,
                active,
                deviceId,
                toggleType
        );
    }
}