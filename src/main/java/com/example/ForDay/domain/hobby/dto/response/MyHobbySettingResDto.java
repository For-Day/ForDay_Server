package com.example.ForDay.domain.hobby.dto.response;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyHobbySettingResDto {
    private List<HobbyDto> hobbies;


    public static class HobbyDto {
        private Long hobbyId;
        private String hobbyName;
        private Integer hobbyTimeMinutes;
        private Integer executionCount;
        private Integer goalDays;
    }
}
