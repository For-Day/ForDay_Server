package com.example.ForDay.domain.hobby.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityDto {
    private Long activityId;
    private String topic;
    private String content;
    private String description;
}
