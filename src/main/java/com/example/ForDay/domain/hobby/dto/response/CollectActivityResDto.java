package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.hobby.entity.Hobby;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectActivityResDto {
    private Long hobbyId;
    private String hobbyName;
    private Long activityId;
    private String content;
    private String message;

    private static final String SUCCESS_MESSAGE = "활동담기를 완료했어요.";

    public static CollectActivityResDto of(Hobby hobby, Activity activity) {
        return new CollectActivityResDto(
                hobby.getId(),
                hobby.getHobbyName(),
                activity.getId(),
                activity.getContent(),
                SUCCESS_MESSAGE
        );
    }
}
