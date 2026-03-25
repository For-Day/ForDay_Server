package com.example.ForDay.domain.hobby.dto.request;

import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivitySummaryRequest {
    private String userId;
    private Long userHobbyId;
    private String hobbyName;

    public static ActivitySummaryRequest of(String userId, Long userHobbyId, String hobbyName) {
        return ActivitySummaryRequest.builder()
                .userId(userId)
                .userHobbyId(userHobbyId)
                .hobbyName(hobbyName)
                .build();
    }
}
