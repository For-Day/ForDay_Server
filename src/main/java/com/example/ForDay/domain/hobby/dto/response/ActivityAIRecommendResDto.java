package com.example.ForDay.domain.hobby.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityAIRecommendResDto {
    private String message;
    private int aiCallCount;
    private int aiCallLimit;
    private String recommendedText;
    private List<ActivityDto> activities;

    public static ActivityAIRecommendResDto of(
            int currentCount,
            int maxLimit,
            String summary,
            List<ActivityDto> activities) {

        return ActivityAIRecommendResDto.builder()
                .message("AI가 취미 활동을 추천했습니다.")
                .aiCallCount(currentCount)
                .aiCallLimit(maxLimit)
                .recommendedText(summary)
                .activities(activities)
                .build();
    }
}
