package com.example.ForDay.domain.hobby.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateHobbyResDto {
    private Long hobbyId;
    private Long hobbyInfoId;
    private String hobbyName;
    private String hobbyPurpose;
    private Integer hobbyTimeMinutes;
    private Integer executionCount;
    private Integer goalDays;
}
