package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.global.common.response.message.RecordSuccessCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.RecordSuccessCode.DELETE_RECORD_SUCCESS;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteActivityRecordResDto {
    @Schema(description = "결과 메시지", example = "활동 기록이 삭제되었어요.")
    private String message;

    @Schema(description = "삭제된 활동 기록 ID", example = "2")
    private Long recordId;

    @Schema(description = "S3상에 삭제해야하는 이미지 URL")
    private String deleteImageUrl;

    public static DeleteActivityRecordResDto of(Long recordId, String deleteImageUrl) {
        return new DeleteActivityRecordResDto(
                RecordSuccessCode.DELETE_RECORD_SUCCESS.getMessage(),
                recordId,
                deleteImageUrl
        );
    }
}
