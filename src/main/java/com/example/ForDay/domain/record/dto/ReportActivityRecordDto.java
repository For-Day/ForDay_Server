package com.example.ForDay.domain.record.dto;

import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportActivityRecordDto {
    private Long recordId;
    private ActivityRecord activityRecord;
    private User writer;
    private String writerId;
    private boolean writerDeleted;
    private String writerNickname;
    private RecordVisibility visibility;
}
