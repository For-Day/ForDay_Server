package com.example.ForDay.domain.hobby.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SimpleActivityRecommendReqDto {

    @NotBlank(message = "hobbyName은 필수입니다.")
    private String hobbyName;

    @NotBlank(message = "hobbyPurpose은 필수입니다.")
    private String hobbyPurpose;

    @NotNull(message = "hobbyTimeMinutes는 필수입니다.")
    private Integer hobbyTimeMinutes;
}
