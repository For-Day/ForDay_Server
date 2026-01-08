package com.example.ForDay.global.common.error.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 공통
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),
    TEST_ERROR_CODE(HttpStatus.BAD_REQUEST, "오류가 발생하였습니다."),

    // 카카오 관련
    KAKAO_PROFILE_REQUEST_FAILED(HttpStatus.BAD_REQUEST, "카카오 사용자 정보 조회에 실패했습니다."),

    // 인증/인가 관련
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 리프레시 토큰입니다."),

    // 사용자 관련
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USERNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 사용 중인 사용자 이름입니다."),

    // AI 관련
    AI_CALL_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "AI 최대 호출 횟수를 초과하였습니다."),
    AI_RESPONSE_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "AI 응답 형식이 올바르지 않아 데이터를 처리할 수 없습니다."),
    AI_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "AI 서비스 연결 중에 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),

    // 취미 관련
    HOBBY_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 취미 카드입니다."),
    INVALID_USER_ROLE(HttpStatus.FORBIDDEN, "해당 작업을 수행할 수 있는 권한이 없습니다.");

    private final HttpStatus status;
    private final String message;
}
