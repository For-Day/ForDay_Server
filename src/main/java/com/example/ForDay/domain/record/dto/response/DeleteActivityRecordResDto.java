package com.example.ForDay.domain.record.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteActivityRecordResDto {
    @Schema(description = "결과 메시지", example = "활동 기록이 정상적으로 삭제되었습니다.")
    private String message;

    @Schema(description = "삭제된 활동 기록 ID", example = "2")
    private Long recordId;

    @Schema(description = "S3상에 삭제해야하는 이미지 URL")
    private String deleteImageUrl;
}
