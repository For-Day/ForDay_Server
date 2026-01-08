package com.example.ForDay.domain.hobby.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OthersActivityRecommendResDto {
    private String message;
    private List<ActivityDto> activities;

    @Data
    @AllArgsConstructor
    public static class ActivityDto {
        private Long id;
        private String content;
    }
}
