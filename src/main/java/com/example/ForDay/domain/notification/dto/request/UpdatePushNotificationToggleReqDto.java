package com.example.ForDay.domain.notification.dto.request;

import com.example.ForDay.domain.notification.type.ToggleType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePushNotificationToggleReqDto {
    private boolean active;
    private ToggleType toggleType;
}