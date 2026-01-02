package com.example.ForDay.domain.auth.dto.response;

import com.example.ForDay.domain.user.type.SocialType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "카카오 로그인 응답 DTO")
public class LoginResDto {
    @Schema(description = "Access Token (API 호출 시 Authorization 헤더에 사용)", example = "eyJhbGciOiJIUzI1...")
    private String accessToken;

    @Schema(description = "Refresh Token (재발급 시 사용)", example = "eyJhbGciOiJIUzI1...")
    private String refreshToken;

    @Schema(description = "신규 가입 여부", example = "true")
    private boolean newUser;

    @Schema(description = "가입 유형", example = "KAKAO")
    private SocialType socialType;
}
