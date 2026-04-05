package com.example.ForDay.global.common.response.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationSuccessCode {
    ACTIVE_NOTIFICATION_SUCCESS("푸시 알림이 활성화되었습니다."),
    INACTIVE_NOTIFICATION_SUCCESS("푸시 알림이 비활성화되었습니다."),
    ALREADY_SAME_STATUS("이미 해당 상태입니다."),
    SEND_NOTIFICATION_SUCCESS("성공적으로 푸시 알림이 전송되었습니다."),
    REQUEST_NOTIFICATION_PERMISSION("알림을 놓치지 않도록 알림 권한을 허용해주세요.");

    private final String message;
}