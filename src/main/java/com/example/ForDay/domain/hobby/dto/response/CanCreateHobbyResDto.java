package com.example.ForDay.domain.hobby.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "취미 생성 가능 여부 응답")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CanCreateHobbyResDto {
    @Schema(description = "응답 메시지", example = "이미 등록한 취미입니다.")
    private String message;

    @Schema(description = "생성 가능 여부 (true: 가능, false: 중복)", example = "false")
    private boolean availability;
}
