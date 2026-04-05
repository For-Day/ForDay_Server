package com.example.ForDay.global.common.response.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ActivitySuccessCode {
    RECORD_ACTIVITY_SUCCESS("오늘의 활동 기록이 정상적으로 작성되었습니다"),
    ACTIVITY_UPDATE_SUCCESS("활동이 정상적으로 수정되었습니다."),
    ACTIVITY_DELETE_SUCCESS("활동이 삭제되었습니다."),
    COLLECT_ACTIVITY_SUCCESS("활동담기를 완료했어요.");

    private final String message;
}