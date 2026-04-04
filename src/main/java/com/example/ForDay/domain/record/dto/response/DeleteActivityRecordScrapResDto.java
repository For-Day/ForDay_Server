package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.global.common.response.message.RecordSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.RecordSuccessCode.DELETE_SCRAP_SUCCESS;
import static com.example.ForDay.global.common.response.message.RecordSuccessCode.NOT_EXISTS_SCRAP;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteActivityRecordScrapResDto {
    private String message;
    private Long recordId;
    private boolean scraped;

    public static DeleteActivityRecordScrapResDto notExistScrap(Long recordId) {
        return new DeleteActivityRecordScrapResDto(
                RecordSuccessCode.NOT_EXISTS_SCRAP.getMessage(),
                recordId,
                false
        );
    }

    public static DeleteActivityRecordScrapResDto deleteScrap(Long recordId) {
        return new DeleteActivityRecordScrapResDto(
                RecordSuccessCode.DELETE_SCRAP_SUCCESS.getMessage(),
                recordId,
                true
        );
    }
}
