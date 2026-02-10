package com.example.ForDay.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "애플 로그인 요청 DTO")
public class AppleLoginReqDto {

    @NotBlank(message = "Apple authorization code는 필수입니다.")
    private String code;
}
