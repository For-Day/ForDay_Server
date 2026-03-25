package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.domain.activity.entity.OtherActivity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import static com.example.ForDay.global.common.response.message.HobbySuccessMessage.OTHER_HOBBY_MANNY_ACTIVITY_SUCCESS;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OthersActivityRecommendResDto {
    private String message;
    private List<ActivityDto> activities;

    public static  OthersActivityRecommendResDto of(List<ActivityDto> activities) {
        return new OthersActivityRecommendResDto(OTHER_HOBBY_MANNY_ACTIVITY_SUCCESS, activities);
    }

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
