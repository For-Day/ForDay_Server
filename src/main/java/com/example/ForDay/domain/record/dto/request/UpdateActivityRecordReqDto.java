package com.example.ForDay.domain.record.dto.request;

import com.example.ForDay.domain.record.type.RecordVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateActivityRecordReqDto {
    @Schema(description = "수정할 활동 ID", example = "2")
    private Long activityId;

    @Schema(description = "스티커 이미지 파일명", example = "sad.jpg")
    @NotBlank(message = "스티커는 필수입니다.")
    private String sticker;

    @Schema(description = "스티커 이미지 파일명", example = "sad.jpg")
    @Size(max = 100, message = "메모는 최대 100자까지 입력 가능합니다.")
    private String memo;

    @Schema(description = "S3 이미지 URL", example = "https://forday-s3-bucket.s3...")
    private String imageUrl;

    @Schema(description = "공개 범위 (PUBLIC, FRIEND, PRIVATE)", example = "FRIEND")
    @NotNull(message = "공개 여부 설정은 필수입니다.")
    private RecordVisibility visibility;
}
