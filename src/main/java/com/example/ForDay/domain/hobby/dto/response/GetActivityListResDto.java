package com.example.ForDay.domain.hobby.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetActivityListResDto {
    private List<ActivityDto> activities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityDto {
        private Long activityId;
        private String content;
        private boolean aiRecommended;
        private boolean deletable;
        private List<StickerDto> stickers;

        public ActivityDto(Long activityId, String content, boolean aiRecommended, Integer collectedStickerNum) {
            this.activityId = activityId;
            this.content = content;
            this.aiRecommended = aiRecommended;
            this.deletable = (collectedStickerNum != null && collectedStickerNum == 0);;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StickerDto {
        private Long activityRecordId;
        private String sticker;
    }
}
