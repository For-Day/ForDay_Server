package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.type.RecordVisibility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateActivityRecordResDto {
    private String message;
    private Long activityId;
    private String activityContent;
    private String sticker;
    private String memo;
    private String imageUrl;
    private RecordVisibility visibility;
}
