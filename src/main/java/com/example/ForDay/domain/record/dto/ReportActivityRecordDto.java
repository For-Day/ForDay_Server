package com.example.ForDay.domain.record.dto;

import com.example.ForDay.domain.record.type.RecordVisibility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportActivityRecordDto {
    private Long recordId;
    private String writerId;
    private boolean writerDeleted;
    private String writerNickname;
    private RecordVisibility visibility;
}
