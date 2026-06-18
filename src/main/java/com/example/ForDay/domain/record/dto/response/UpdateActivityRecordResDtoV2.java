package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.type.RecordVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "활동 기록 수정 응답 DTO V2")
public class UpdateActivityRecordResDtoV2 {

    @Schema(description = "결과 메시지", example = "활동 기록이 정상적으로 수정되었습니다.")
    private String message;

    @Schema(description = "수정된 활동 기록 ID", example = "42")
    private Long activityRecordId;

    @Schema(description = "수정된 활동 ID", example = "2")
    private Long activityId;

    @Schema(description = "수정된 활동 내용", example = "오늘의 독서")
    private String activityContent;

    @Schema(description = "스티커 이름", example = "HAPPY")
    private String sticker;

    @Schema(description = "메모", example = "오늘은 1시간 독서했다.")
    private String memo;

    @Schema(description = "공개 범위 (PUBLIC / FRIEND / PRIVATE)", example = "PUBLIC")
    private RecordVisibility visibility;

    @Schema(description = "첨부 이미지 목록")
    private List<ActivityRecordResDto.ImageInfo> images;
}
