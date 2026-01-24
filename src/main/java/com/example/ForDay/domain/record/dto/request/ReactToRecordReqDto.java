package com.example.ForDay.domain.record.dto.request;

import com.example.ForDay.domain.record.type.RecordReactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReactToRecordReqDto {
    private RecordReactionType reactionType;
}
