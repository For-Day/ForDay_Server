package com.example.ForDay.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.UserSuccessMessage.ALREADY_USED_NICKNAME;
import static com.example.ForDay.global.common.response.message.UserSuccessMessage.ENABLE_USE_NICKNAME;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NicknameCheckResDto {
    private String nickname;
    private boolean isAvailable;
    private String message;

    public static NicknameCheckResDto alreadyUsedNickname(String nickname) {
        return new NicknameCheckResDto(
                nickname,
                false,
                ALREADY_USED_NICKNAME
        );
    }

    public static NicknameCheckResDto canUseNickname(String nickname) {
        return new NicknameCheckResDto(
                nickname,
                true,
                ENABLE_USE_NICKNAME
        );
    }
}
