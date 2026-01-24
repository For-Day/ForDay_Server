package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.type.RecordReactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CancelReactToRecordResDto {
    private String message;
    private RecordReactionType reactionType;
    private Long recordId;
}
