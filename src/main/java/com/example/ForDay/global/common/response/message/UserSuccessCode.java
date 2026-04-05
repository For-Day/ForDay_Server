package com.example.ForDay.global.common.response.message;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserSuccessCode {
    // 닉네임 관련
    ENABLE_USE_NICKNAME("사용 가능한 닉네임입니다."),
    ALREADY_USED_NICKNAME("이미 사용 중인 닉네임입니다."),
    NICKNAME_REGISTER_SUCCESS("사용자 이름이 성공적으로 등록되었습니다."),
    ALREADY_SAME_PROFILE_IMAGE("이미 동일한 프로필 이미지로 설정되어 있습니다."),
    UPDATE_PROFILE_IMAGE_SUCCESS("프로필 이미지가 성공적으로 변경되었습니다."),
    AGREE_TERMS_SUCCESS("약관 동의가 정상적으로 수집되었습니다.");
    private final String message;
}
