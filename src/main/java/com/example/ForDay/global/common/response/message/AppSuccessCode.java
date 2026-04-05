package com.example.ForDay.global.common.response.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AppSuccessCode {
    DELETE_S3_IMAGE_SUCCESS("이미지가 성공적으로 삭제되었습니다."),
    UPDATE_FCM_TOKEN_SUCCESS("FCM 토큰이 성공적으로 갱신되었습니다.");

    private final String message;
}