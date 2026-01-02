package com.example.ForDay.domain.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "토큰 재발급 응답 DTO")
public class RefreshResDto {

    @Schema(description = "Access Token (API 호출 시 Authorization 헤더에 사용)", example = "eyJhbGciOiJIUzI1...")
    private String accessToken;

    @Schema(description = "Refresh Token (재발급 시 사용)", example = "eyJhbGciOiJIUzI1...")
    private String refreshToken;
}
