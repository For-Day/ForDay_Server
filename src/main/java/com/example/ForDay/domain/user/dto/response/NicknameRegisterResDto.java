package com.example.ForDay.domain.user.dto.response;

import com.example.ForDay.global.common.response.message.UserSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.UserSuccessCode.NICKNAME_REGISTER_SUCCESS;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NicknameRegisterResDto {
    private String message;
    private String nickname;

    public static NicknameRegisterResDto from(String nickname) {
        return new NicknameRegisterResDto(
                UserSuccessCode.NICKNAME_REGISTER_SUCCESS.getMessage(),
                nickname
        );
    }
}
