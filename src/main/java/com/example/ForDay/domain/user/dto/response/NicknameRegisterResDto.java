package com.example.ForDay.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.UserSuccessMessage.NICKNAME_REGISTER_SUCCESS;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NicknameRegisterResDto {
    private String message;
    private String nickname;

    public static NicknameRegisterResDto from(String nickname) {
        return new NicknameRegisterResDto(
                NICKNAME_REGISTER_SUCCESS,
                nickname
        );
    }
}
