package com.example.ForDay.domain.app.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateFcmTokenReqDto {
    private String fcmToken;
    private String deviceId;
}