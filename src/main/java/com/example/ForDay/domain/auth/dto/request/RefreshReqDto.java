package com.example.ForDay.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "토큰 재발급 요청 DTO")
public class RefreshReqDto {

    @NotBlank(message = "리프레시 토큰은 필수 값입니다.")
    @Schema(
            description = "Refresh Token (만료된 Access Token 재발급 시 사용)",
            required = true
    )
    private String refreshToken;
}
