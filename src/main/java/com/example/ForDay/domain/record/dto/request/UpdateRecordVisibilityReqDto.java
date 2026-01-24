package com.example.ForDay.domain.record.dto.request;

import com.example.ForDay.domain.record.type.RecordVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "기록 공개 범위 수정 요청")
public class UpdateRecordVisibilityReqDto {

    @Schema(description = "변경할 공개 범위 (PUBLIC, FRIEND, PRIVATE)", example = "FRIEND", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "변경할 공개 범위를 선택해주세요.")
    private RecordVisibility visibility;
}