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
}
