package com.example.ForDay.domain.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.AppSuccessMessage.UPDATE_FCM_TOKEN_SUCCESS;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateFcmTokenResDto {
    private String message;
    private String fcmToken;
    private String deviceId;

    public static UpdateFcmTokenResDto of(String deviceId, String newFcmToken) {
        return new UpdateFcmTokenResDto(
                UPDATE_FCM_TOKEN_SUCCESS,
                newFcmToken,
                deviceId
        );
    }
}