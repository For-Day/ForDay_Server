package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.type.RecordVisibility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.RecordSuccessMessage.ALREADY_RECORD_VISIBILITY;
import static com.example.ForDay.global.common.response.message.RecordSuccessMessage.UPDATE_RECORD_VISIBILITY_SUCCESS;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRecordVisibilityResDto {
    private String message;
    private RecordVisibility previousVisibility;
    private RecordVisibility newVisibility;

    public static UpdateRecordVisibilityResDto alreadyVisibility(RecordVisibility prev, RecordVisibility next) {
        return new UpdateRecordVisibilityResDto(
                ALREADY_RECORD_VISIBILITY,
                prev,
                next
        );
    }

    public static UpdateRecordVisibilityResDto updateVisibility(RecordVisibility prev, RecordVisibility next) {
        return new UpdateRecordVisibilityResDto(
                UPDATE_RECORD_VISIBILITY_SUCCESS,
                prev,
                next
        );
    }
}
