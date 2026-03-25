package com.example.ForDay.domain.hobby.dto.request;

import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FastAPIRecommendReqDto {
    private String userId;
    private int userHobbyId;
    private String hobbyName;
    private String hobbyPurpose;
    private int hobbyTimeMinutes;
    private int executionCount;
    private int goalDays;

    public static FastAPIRecommendReqDto from(User user, Hobby hobby) {
        return FastAPIRecommendReqDto.builder()
                .userId(user.getId())
                .userHobbyId(hobby.getId().intValue())
                .hobbyName(hobby.getHobbyName())
                .hobbyPurpose(hobby.getHobbyPurpose())
                .hobbyTimeMinutes(hobby.getHobbyTimeMinutes())
                .executionCount(hobby.getExecutionCount())
                .goalDays(hobby.getGoalDays() != null ? hobby.getGoalDays() : 0)
                .build();
    }
}
