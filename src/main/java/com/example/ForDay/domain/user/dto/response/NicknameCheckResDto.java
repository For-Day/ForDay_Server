package com.example.ForDay.domain.user.dto.response;

import com.example.ForDay.global.common.response.message.UserSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.UserSuccessCode.ALREADY_USED_NICKNAME;
import static com.example.ForDay.global.common.response.message.UserSuccessCode.ENABLE_USE_NICKNAME;

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
                UserSuccessCode.ALREADY_USED_NICKNAME.getMessage()
        );
    }

    public static NicknameCheckResDto canUseNickname(String nickname) {
        return new NicknameCheckResDto(
                nickname,
                true,
                UserSuccessCode.ENABLE_USE_NICKNAME.getMessage()
        );
    }
}
