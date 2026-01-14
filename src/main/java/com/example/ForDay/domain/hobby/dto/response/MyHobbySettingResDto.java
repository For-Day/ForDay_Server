package com.example.ForDay.domain.hobby.dto.response;


import com.example.ForDay.domain.hobby.type.HobbyStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyHobbySettingResDto {
    private HobbyStatus currentHobbyStatus;
    private Long inProgressHobbyCount;
    private Long archivedHobbyCount;
    private List<HobbyDto> hobbies;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HobbyDto {
        private Long hobbyId;
        private String hobbyName;
        private Integer hobbyTimeMinutes;
        private Integer executionCount;
        private Integer goalDays;
    }
}
