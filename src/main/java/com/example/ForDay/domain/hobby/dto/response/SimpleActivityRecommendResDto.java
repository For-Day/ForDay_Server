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
public class SimpleActivityRecommendResDto {
    private List<ActivityDto> activities;

    public static SimpleActivityRecommendResDto of(List<ActivityDto> activities) {
        return SimpleActivityRecommendResDto.builder()
                .activities(activities)
                .build();
    }
}
