package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.domain.hobby.entity.Hobby;
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

    public static UpdateHobbyResDto from(Hobby hobby) {
        return new UpdateHobbyResDto(
                hobby.getId(),
                hobby.getHobbyInfoId(),
                hobby.getHobbyName(),
                hobby.getHobbyPurpose(),
                hobby.getHobbyTimeMinutes(),
                hobby.getExecutionCount(),
                hobby.getGoalDays()
        );
    }
}
