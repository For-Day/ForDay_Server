package com.example.ForDay.domain.auth.dto.request;

import com.example.ForDay.global.firebase.type.DeviceType;
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
            description = "카카오 액세스 토큰"
    )
    @NotBlank(message = "카카오 액세스 토큰은 필수 값입니다.")
    private String kakaoAccessToken;

    private String fcmToken;
    private String deviceId;
    private DeviceType deviceType;
}
