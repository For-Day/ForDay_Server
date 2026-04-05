package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.global.common.response.message.RecordSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.RecordSuccessCode.ALREADY_RECORD_VISIBILITY;
import static com.example.ForDay.global.common.response.message.RecordSuccessCode.UPDATE_RECORD_VISIBILITY_SUCCESS;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRecordVisibilityResDto {
    private String message;
    private RecordVisibility previousVisibility;
    private RecordVisibility newVisibility;

    public static UpdateRecordVisibilityResDto alreadyVisibility(RecordVisibility prev, RecordVisibility next) {
        return new UpdateRecordVisibilityResDto(
                RecordSuccessCode.ALREADY_RECORD_VISIBILITY.getMessage(),
                prev,
                next
        );
    }

    public static UpdateRecordVisibilityResDto updateVisibility(RecordVisibility prev, RecordVisibility next) {
        return new UpdateRecordVisibilityResDto(
                RecordSuccessCode.UPDATE_RECORD_VISIBILITY_SUCCESS.getMessage(),
                prev,
                next
        );
    }
}
