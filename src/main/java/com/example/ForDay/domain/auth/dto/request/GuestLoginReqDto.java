package com.example.ForDay.domain.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "게스트 로그인 요청 DTO")
public class GuestLoginReqDto {
    @Schema(
            description = "게스트 사용자 식별자. " +
                    "처음 로그인 시에는 빈 문자열 또는 null을 전달합니다. " +
                    "로컬에 저장된 guestUserId가 있으면 해당 값을 전달합니다."
    )
    private String guestUserId;
}
