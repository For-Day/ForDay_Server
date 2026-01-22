package com.example.ForDay.domain.hobby.dto.request;

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
}
