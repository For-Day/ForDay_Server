package com.example.ForDay.domain.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetPushNotificationToggleResDto {
    private boolean appPushEnabled;
    private boolean recordPushEnabled;

    public static GetPushNotificationToggleResDto of(boolean appPushEnabled, boolean recordPushEnabled) {
        return new GetPushNotificationToggleResDto(
                appPushEnabled,
                recordPushEnabled
        );
    }
}