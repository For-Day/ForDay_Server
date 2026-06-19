package com.example.ForDay.domain.activity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetRecentActivityListResDto {
    private List<ActivityDto> activities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityDto {
        private Long activityId;
        private String content;
        private Integer collectedStickerNum;
        private LocalDateTime lastRecordedAt;
    }
}
