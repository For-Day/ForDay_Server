package com.example.ForDay.global.common.response.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FriendSuccessCode {
    // 친구 관련
    ALREADY_FRIEND("이미 친구 맺기가 되어있습니다."),
    ADD_FRIEND_SUCCESS("성공적으로 친구 맺기가 되었습니다."),
    DELETE_FRIEND_SUCCESS("성공적으로 친구 관계를 삭제했습니다."),
    FRIEND_LIST_GET_SUCCESS("친구 목록이 성공적으로 조회되었습니다."),

    // 차단 및 신고 관련
    ALREADY_BLOCKED("이미 차단된 상태입니다."),
    ALREADY_REPORTED("이미 신고된 상태입니다."),
    REPORT_FRIEND_SUCCESS("신고가 완료되었습니다.");

    private final String message;
}