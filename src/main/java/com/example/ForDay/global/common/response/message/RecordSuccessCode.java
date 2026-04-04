package com.example.ForDay.global.common.response.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecordSuccessCode {
    // 반응 및 리액션
    REACT_TO_RECORD_SUCCESS("반응이 정상적으로 등록되었습니다."),
    CANCEL_REACT_RECORD_SUCCESS("리액션이 정상적으로 취소되었습니다."),

    // 공개 범위 설정
    ALREADY_RECORD_VISIBILITY("이미 설정된 공개 범위입니다."),
    UPDATE_RECORD_VISIBILITY_SUCCESS("공개 범위가 정상적으로 변경되었습니다."),

    // 활동 기록 관리
    UPDATE_RECORD_SUCCESS("활동 기록이 정상적으로 수정되었습니다."),
    DELETE_RECORD_SUCCESS("활동 기록이 삭제되었어요."),

    // 스크랩 관련
    RECORD_SCRAP_SUCCESS("스크랩을 완료했어요."),
    DELETE_SCRAP_SUCCESS("스크랩 취소가 완료되었습니다."),
    NOT_EXISTS_SCRAP("스크랩이 존재하지 않거나 이미 삭제되었습니다."),
    REPORT_RECORD_SUCCESS("기록이 정상적으로 신고되었습니다.");

    private final String message;
}