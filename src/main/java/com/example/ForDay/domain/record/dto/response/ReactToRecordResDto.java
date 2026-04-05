package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.global.common.response.message.RecordSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.RecordSuccessCode.REACT_TO_RECORD_SUCCESS;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReactToRecordResDto {
    private String message;
    private RecordReactionType reactionType;
    private Long recordId;

    public static ReactToRecordResDto of(RecordReactionType reactionType, Long recordId) {
        return new ReactToRecordResDto(
                RecordSuccessCode.REACT_TO_RECORD_SUCCESS.getMessage(),
                reactionType,
                recordId
        );
    }
}
