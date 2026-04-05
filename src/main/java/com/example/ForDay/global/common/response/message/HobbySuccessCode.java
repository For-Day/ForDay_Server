package com.example.ForDay.global.common.response.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HobbySuccessCode {
    // 생성 및 활동 관련
    CREATE_HOBBY_SUCCESS("취미가 성공적으로 생성되었습니다."),
    ADD_ACTIVITY_SUCCESS("취미 활동이 정상적으로 생성되었습니다."),
    OTHER_HOBBY_MANNY_ACTIVITY_SUCCESS("다른 하비들이 많이 하는 활동 목록 조회에 성공하셨습니다."),

    // 설정 및 수정 관련
    UPDATE_HOBBY_TIME_SUCCESS("취미 시간이 수정되었습니다."),
    UPDATE_EXECUTION_COUNT_SUCCESS("실행 횟수가 수정되었습니다."),
    UPDATE_GOAL_DAYS_SUCCESS("목표 기간 설정이 수정되었습니다."),
    SET_HOBBY_EXTENSION_SUCCESS("취미 기간 설정이 정상적으로 처리되었습니다."),
    SET_HOBBY_COVER_IMAGE_SUCCESS("대표사진 설정 완료!"),

    // 상태 관련
    ALREADY_HOBBY_STATUS("이미 해당 상태입니다.");

    private final String message;
}
