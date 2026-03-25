package com.example.ForDay.domain.record.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.RecordSuccessMessage.DELETE_SCRAP_SUCCESS;
import static com.example.ForDay.global.common.response.message.RecordSuccessMessage.NOT_EXISTS_SCRAP;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeleteActivityRecordScrapResDto {
    private String message;
    private Long recordId;
    private boolean scraped;

    public static DeleteActivityRecordScrapResDto notExistScrap(Long recordId) {
        return new DeleteActivityRecordScrapResDto(
                NOT_EXISTS_SCRAP,
                recordId,
                false
        );
    }

    public static DeleteActivityRecordScrapResDto deleteScrap(Long recordId) {
        return new DeleteActivityRecordScrapResDto(
                DELETE_SCRAP_SUCCESS,
                recordId,
                true
        );
    }
}
