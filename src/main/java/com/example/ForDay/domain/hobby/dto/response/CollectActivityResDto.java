package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.hobby.entity.Hobby;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.ActivitySuccessMessage.COLLECT_ACTIVITY_SUCCESS;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectActivityResDto {
    private Long hobbyId;
    private String hobbyName;
    private Long activityId;
    private String content;
    private String message;

    public static CollectActivityResDto of(Hobby hobby, Activity activity) {
        return new CollectActivityResDto(
                hobby.getId(),
                hobby.getHobbyName(),
                activity.getId(),
                activity.getContent(),
                COLLECT_ACTIVITY_SUCCESS
        );
    }
}
