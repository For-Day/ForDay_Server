package com.example.ForDay.domain.record.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.RecordSuccessMessage.RECORD_SCRAP_SUCCESS;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddActivityRecordScrapResDto {
    private String message;
    private Long recordId;
    private boolean scraped;

    public static AddActivityRecordScrapResDto from(Long recordId) {
        return new AddActivityRecordScrapResDto(
                RECORD_SCRAP_SUCCESS,
                recordId,
                true
        );
    }
}
