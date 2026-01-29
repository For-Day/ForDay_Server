package com.example.ForDay.domain.record.dto;

import com.example.ForDay.domain.record.type.RecordVisibility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityRecordWithUserDto {
    private Long activityRecordId;
    private RecordVisibility visibility;
    private String writerId;
    private boolean writerDeleted;
}
