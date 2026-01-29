package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.type.RecordVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateActivityRecordResDto {

    @Schema(description = "결과 메시지", example = "활동 기록이 정상적으로 수정되었습니다.")
    private String message;

    @Schema(description = "수정된 활동 ID", example = "2")
    private Long activityId;

    @Schema(description = "수정된 활동 내용", example = "지금 당장 눈 앞에 있는 것 그리기")
    private String activityContent;

    @Schema(description = "스티커", example = "sad.jpg")
    private String sticker;

    @Schema(description = "메모", example = "수정내용")
    private String memo;

    @Schema(description = "이미지 URL", example = "https://forday-s3-bucket.s3...")
    private String imageUrl;

    @Schema(description = "공개 범위", example = "FRIEND")
    private RecordVisibility visibility;
}
