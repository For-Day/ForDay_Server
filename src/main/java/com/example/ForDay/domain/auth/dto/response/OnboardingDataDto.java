package com.example.ForDay.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingDataDto {
    private Long id;
    private Long hobbyCardId;
    private String hobbyName;
    private String hobbyPurpose;
    private Integer hobbyTimeMinutes;
    private Integer executionCount;
    private boolean isDurationSet;

    public OnboardingDataDto(Long id, Long hobbyCardId, String hobbyName, String hobbyPurpose, Integer hobbyTimeMinutes, Integer executionCount, Integer goalDays) {
        this.id = id;
        this.hobbyCardId = hobbyCardId;
        this.hobbyName = hobbyName;
        this.hobbyPurpose = hobbyPurpose;
        this.hobbyTimeMinutes = hobbyTimeMinutes;
        this.executionCount = executionCount;
        this.isDurationSet = goalDays != null;
    }
}
