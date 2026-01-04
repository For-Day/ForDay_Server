package com.example.ForDay.domain.user.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NicknameRegisterReqDto {

    @NotBlank(message = "닉네임을 입력해주세요.")
    @Size(max = 10, message = "닉네임은 10자 이내로 입력해주세요.")
    @Pattern(
            regexp = "^[가-힣a-zA-Z0-9]+$",
            message = "닉네임은 한글, 영어, 숫자만 사용할 수 있습니다."
    )
    private String nickname;
}
