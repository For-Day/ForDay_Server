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

    // 애플 관련
    APPLE_PROFILE_REQUEST_FAILED(HttpStatus.BAD_REQUEST, "애플 사용자 정보 조회에 실패했습니다."),
    APPLE_LOGIN_FAILED(HttpStatus.BAD_REQUEST, "애플 로그인에 실패했습니다."),

    // 인증/인가 관련
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    LOGIN_EXPIRED(HttpStatus.UNAUTHORIZED, "로그인이 만료되었습니다. 다시 로그인해주세요."),

    // 사용자 관련
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    USERNAME_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "이미 사용 중인 사용자 이름입니다."),
    NO_GUEST_ACCESS(HttpStatus.FORBIDDEN, "게스트만 접근 가능합니다." ),

    // AI 관련
    AI_CALL_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "AI 최대 호출 횟수를 초과하였습니다."),
    AI_RESPONSE_INVALID(HttpStatus.UNPROCESSABLE_ENTITY, "AI 응답 형식이 올바르지 않아 데이터를 처리할 수 없습니다."),
    AI_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "AI 서비스 연결 중에 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
    AI_RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AI 요청이 너무 많아 일시적으로 처리할 수 없습니다. 잠시 후 다시 시도해주세요."),


    // 취미 관련
    HOBBY_INFO_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 취미 카드입니다."),
    INVALID_USER_ROLE(HttpStatus.FORBIDDEN, "해당 작업을 수행할 수 있는 권한이 없습니다."),
    HOBBY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 취미입니다."),
    NOT_HOBBY_OWNER(HttpStatus.FORBIDDEN, "취미 소유자가 아닙니다."),
    MAX_IN_PROGRESS_HOBBY_EXCEEDED(HttpStatus.BAD_REQUEST, "이미 진행 중인 취미는 최대 2개까지 등록할 수 있습니다."),
    INVALID_HOBBY_STATUS(HttpStatus.BAD_REQUEST, "현재 취미 상태에서는 해당 작업을 수행할 수 없습니다."),
    STICKER_COMPLETION_REACHED(HttpStatus.BAD_REQUEST, "해당 취미의 스티커 수가 이미 최대치에 도달했습니다."),
    INVALID_HOBBY_EXTENSION_TYPE(HttpStatus.BAD_REQUEST, "유효하지 않은 취미 기간 설정 타입입니다."),
    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST,"요청한 페이지가 존재하지 않습니다."),
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "입력 값이 유효하지 않습니다."),
    ALREADY_HAVE_HOBBY(HttpStatus.CONFLICT, "이미 가지고 있는 취미입니다."),

    // 활동 관련
    ACTIVITY_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 활동입니다."),
    NOT_ACTIVITY_OWNER(HttpStatus.FORBIDDEN, "활동 소유자가 아닙니다."),
    ACTIVITY_NOT_DELETABLE(HttpStatus.BAD_REQUEST, "삭제할 수 없는 활동입니다."),
    HOBBY_PERIOD_NOT_SET(HttpStatus.BAD_REQUEST, "기간 설정이 없는 취미입니다."),
    HOBBY_STICKER_NOT_ENOUGH(HttpStatus.BAD_REQUEST, "스티커가 66개 이상 채워지지 않았습니다."),

    // 활동 기록 관련
    ALREADY_RECORDED_TODAY(HttpStatus.BAD_REQUEST, "오늘 해당 취미에 대한 활동 기록을 이미 작성하였습니다."),
    ACTIVITY_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 활동 기록입니다."),
    PRIVATE_RECORD(HttpStatus.FORBIDDEN, "이 글은 작성자에게만 권한이 있습니다."),
    FRIEND_ONLY_ACCESS(HttpStatus.FORBIDDEN, "이 글은 친구에게만 접근 권한이 있습니다."),
    NOT_ACTIVITY_RECORD_OWNER(HttpStatus.FORBIDDEN, "활동 기록 소유자가 아닙니다."),
    DUPLICATE_REACTION(HttpStatus.BAD_REQUEST, "해당 기록에는 이미 같은 반응을 하셨습니다."),
    REACTION_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 리액션을 찾을 수 없거나 이미 취소되었습니다."),
    DUPLICATE_SCRAP(HttpStatus.BAD_REQUEST, "해당 기록에는 이미 스크랩을 하셨습니다."),
    ALREADY_RECORD_REPORTED(HttpStatus.CONFLICT, "해당 기록에 이미 신고하였습니다."),
    ALREADY_DELETED_RECORD(HttpStatus.BAD_REQUEST,"이미 삭제된 기록입니다."),

    // s3 관련
    S3_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "S3에 해당 이미지가 존재하지 않습니다. 업로드 여부를 확인해주세요."),
    DUPLICATE_HOBBY_REQUEST(HttpStatus.CONFLICT, "같은 취미에 대한 충복 요청입니다. (닉네임 설정을 완료하세요.)"),
    INVALID_IMAGE_SOURCE(HttpStatus.BAD_REQUEST, "유효하지 않은 이미지 리소스입니다."),

    // 친구 관련
    CANNOT_FOLLOW_SELF(HttpStatus.BAD_REQUEST, "자기 자신에게 친구 맺기를 할 수 없습니다."),
    FRIEND_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 친구 관계입니다."),
    CANNOT_BLOCK_SELF(HttpStatus.BAD_REQUEST, "자기 자신을 차단할 수 없습니다."),

    // 검색어 관련
    KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 검색어입니다."),
    ;

    private final HttpStatus status;
    private final String message;

}
