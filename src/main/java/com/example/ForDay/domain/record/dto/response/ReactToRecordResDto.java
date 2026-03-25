package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.type.RecordReactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.RecordSuccessMessage.REACT_TO_RECORD_SUCCESS;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReactToRecordResDto {
    private String message;
    private RecordReactionType reactionType;
    private Long recordId;

    public static ReactToRecordResDto of(RecordReactionType reactionType, Long recordId) {
        return new ReactToRecordResDto(
                REACT_TO_RECORD_SUCCESS,
                reactionType,
                recordId
        );
    }
}
