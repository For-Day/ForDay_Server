package com.example.ForDay.domain.hobby.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityAIRecommendResDto {
    private String message;
    private int aiCallCount;
    private int aiCallLimit;
    private String recommendedText;
    private List<ActivityDto> activities;

    @Data
    @AllArgsConstructor
    public static class ActivityDto {
        private Long activityId;
        private String topic;
        private String content;
        private String description;
    }
}
