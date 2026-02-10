package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.domain.activity.entity.OtherActivity;
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
    @NoArgsConstructor
    public static class ActivityDto {
        private Long id;
        private String content;

        public static ActivityDto from(OtherActivity activity) {
            ActivityDto dto = new ActivityDto();
            dto.id = activity.getId();
            dto.content = activity.getContent();

            return dto;
        }
    }
}
