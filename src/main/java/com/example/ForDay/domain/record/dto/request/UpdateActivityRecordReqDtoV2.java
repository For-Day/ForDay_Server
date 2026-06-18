package com.example.ForDay.domain.record.dto.request;

import com.example.ForDay.domain.record.type.RecordVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "활동 기록 수정 요청 DTO V2")
public class UpdateActivityRecordReqDtoV2 {

    @Schema(description = "수정할 활동 ID (기존 활동으로 변경 시 입력)", example = "2")
    private Long activityId;

    @Schema(description = "스티커 이름", example = "HAPPY")
    @NotBlank(message = "스티커는 필수입니다.")
    private String sticker;

    @Schema(description = "메모 (최대 100자)", example = "오늘은 1시간 독서했다.")
    @Size(max = 100, message = "메모는 최대 100자까지 입력 가능합니다.")
    private String memo;

    @Valid
    @Schema(description = "첨부 이미지 목록 (최대 5장)")
    private List<ActivityRecordReqDtoV2.ActivityImageReqDto> images;

    @Schema(description = "공개 범위 (PUBLIC / FRIEND / PRIVATE)", example = "PUBLIC", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "공개 여부 설정은 필수입니다.")
    private RecordVisibility visibility;
}
