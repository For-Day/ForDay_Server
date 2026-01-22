package com.example.ForDay.domain.hobby.dto.request;

import com.example.ForDay.domain.hobby.type.ExtensionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "취미 기간 설정 요청 DTO")
public class SetHobbyExtensionReqDto {

    @Schema(
            description = "취미 기간 설정 타입",
            example = "CONTINUE",
            allowableValues = {"CONTINUE", "ARCHIVE"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "취미 기간 설정 타입은 필수입니다.")
    private ExtensionType type;
}
