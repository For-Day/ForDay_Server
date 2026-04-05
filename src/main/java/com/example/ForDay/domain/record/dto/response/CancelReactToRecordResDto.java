package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.global.common.response.message.RecordSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.RecordSuccessCode.CANCEL_REACT_RECORD_SUCCESS;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancelReactToRecordResDto {
    private String message;
    private RecordReactionType reactionType;
    private Long recordId;

    public static CancelReactToRecordResDto of(RecordReactionType type, Long recordId) {
        return new CancelReactToRecordResDto(
                RecordSuccessCode.CANCEL_REACT_RECORD_SUCCESS.getMessage(),
                type,
                recordId
        );
    }
}
