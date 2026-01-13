package com.example.ForDay.domain.hobby.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordActivityResDto {
    private String message;
    private Long activityRecordId;
    private String activityContent;
    private String thumbnailImage;
    private String sticker;
}
