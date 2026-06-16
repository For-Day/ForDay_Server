package com.example.ForDay.domain.record.dto.request;

import com.example.ForDay.domain.record.type.RecordVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "활동 기록하기 요청 DTO")
public class ActivityRecordReqDtoV2 {

    @Schema(description = "기록할 취미 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long hobbyId;

    @Schema(description = "기존 활동 ID. activityContent와 둘 중 하나만 입력. 기존 활동으로 기록 시 사용.", example = "10")
    private Long activityId;

    @Schema(description = "새로 생성할 활동 내용. activityId와 둘 중 하나만 입력. 새 활동 생성 시 사용.", example = "오늘의 독서")
    private String activityContent;

    @Schema(description = "스티커 이름", example = "HAPPY")
    private String sticker;

    @Valid
    @Schema(description = "첨부 이미지 목록 (최대 5장)")
    private List<ActivityImageReqDto> images;

    @Schema(description = "공개 범위 (PUBLIC / FRIEND / PRIVATE)", example = "PUBLIC", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private RecordVisibility visibility;

    @Schema(description = "메모 (최대 100자)", example = "오늘은 1시간 독서했다.")
    private String memo;

    @AssertTrue(message = "activityId가 null이면 activityContent는 필수입니다. activityId가 있으면 activityContent는 null이어야 합니다.")
    public boolean isActivityContentValid() {
        if (activityId == null) {
            return activityContent != null && !activityContent.isBlank();
        } else {
            return activityContent == null;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "첨부 이미지 정보")
    public static class ActivityImageReqDto {

        @NotBlank
        @Schema(description = "S3 이미지 URL", example = "https://s3.example.com/image.jpg", requiredMode = Schema.RequiredMode.REQUIRED)
        private String imageUrl;

        @NotNull
        @Schema(description = "이미지 순서 (1부터 시작, 1번이 썸네일로 사용됨)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        private Integer imageOrder;

        @NotNull
        @Schema(description = "이미지 가로 크기 (px)", example = "1080", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long imageWidth;

        @NotNull
        @Schema(description = "이미지 세로 크기 (px)", example = "1920", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long imageHeight;
    }
}
