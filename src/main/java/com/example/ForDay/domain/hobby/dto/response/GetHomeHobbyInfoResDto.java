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
public class GetHomeHobbyInfoResDto {
    private List<InProgressHobbyDto> inProgressHobbies;
    private ActivityPreviewDto activityPreview;
    private boolean aiCallRemaining;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InProgressHobbyDto {
        private Long hobbyId;
        private String hobbyName;
        private boolean currentHobby;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityPreviewDto {
        private Long activityId;
        private String content;
        private boolean aiRecommended;
    }
}
