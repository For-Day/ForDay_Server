package com.example.ForDay.global.common.response.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthSuccessCode {
    LOGOUT_SUCCESS("로그아웃 되었습니다."),
    WITHDRAW_SUCCESS("회원탈퇴 되었습니다.");

    private final String message;
}