package com.example.ForDay.domain.hobby.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetHobbyActivitiesResDto {
    List<ActivityDto> activities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityDto {
        private Long activityId;
        private String content;
        private boolean aiRecommended;
    }
}
