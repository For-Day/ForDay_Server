package com.example.ForDay.domain.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityRecordCollectInfo {
    private String userId;
    private boolean userDeleted;
    private String content;
}
