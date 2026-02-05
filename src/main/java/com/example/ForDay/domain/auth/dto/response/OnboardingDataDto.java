package com.example.ForDay.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingDataDto {
    private Long id;
    private Long hobbyInfoId;
    private String hobbyName;
    private String hobbyPurpose;
    private Integer hobbyTimeMinutes;
    private Integer executionCount;
    private boolean isDurationSet;

    public OnboardingDataDto(Long id, Long hobbyInfoId, String hobbyName, String hobbyPurpose, Integer hobbyTimeMinutes, Integer executionCount, Integer goalDays) {
        this.id = id;
        this.hobbyInfoId = hobbyInfoId;
        this.hobbyName = hobbyName;
        this.hobbyPurpose = hobbyPurpose;
        this.hobbyTimeMinutes = hobbyTimeMinutes;
        this.executionCount = executionCount;
        this.isDurationSet = goalDays != null;
    }
}
