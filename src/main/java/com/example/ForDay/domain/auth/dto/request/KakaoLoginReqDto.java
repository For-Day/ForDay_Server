package com.example.ForDay.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "카카오 로그인 요청 DTO")
public class KakaoLoginReqDto {

    @Schema(
            description = "카카오 인가 코드 (카카오 SDK / redirect uri에서 받은 값)"
    )
    @NotBlank(message = "인가 코드는 필수 값입니다.")
    private String code; // 인가 코드 (accessToken을 발급받기 위함)
}
