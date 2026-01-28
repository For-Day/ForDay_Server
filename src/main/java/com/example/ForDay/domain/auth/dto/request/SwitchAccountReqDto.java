package com.example.ForDay.domain.auth.dto.request;

import com.example.ForDay.domain.user.type.SocialType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SwitchAccountReqDto {

    @NotNull(message = "소셜 타입(socialType)은 필수 입력 값입니다.")
    private SocialType socialType;

    @NotBlank(message = "소셜 인가 코드(socialCode)는 비어 있을 수 없습니다.")
    private String socialCode;
}