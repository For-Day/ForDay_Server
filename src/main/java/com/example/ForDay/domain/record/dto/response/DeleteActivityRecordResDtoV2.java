package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.global.common.response.message.RecordSuccessCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteActivityRecordResDtoV2 {

    @Schema(description = "결과 메시지", example = "활동 기록이 삭제되었어요.")
    private String message;

    @Schema(description = "삭제된 활동 기록 ID", example = "42")
    private Long recordId;

    @Schema(description = "S3에서 삭제된 이미지 URL 목록")
    private List<String> deleteImageUrls;

    public static DeleteActivityRecordResDtoV2 of(Long recordId, List<String> deleteImageUrls) {
        return new DeleteActivityRecordResDtoV2(
                RecordSuccessCode.DELETE_RECORD_SUCCESS.getMessage(),
                recordId,
                deleteImageUrls
        );
    }
}
