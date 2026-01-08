package com.example.ForDay.domain.hobby.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityAIRecommendReqDto {
    @NotBlank(message = "hobbyName은 필수입니다.")
    private String hobbyName;

    @NotNull(message = "hobbyTimeMinutes는 필수입니다.")
    @Min(value = 1, message = "hobbyTimeMinutes는 1분 이상이어야 합니다.")
    private Integer hobbyTimeMinutes;

    @NotEmpty(message = "hobbyPurposes는 최소 1개 이상 필요합니다.")
    private List<@NotBlank(message = "취미 목적은 빈 값일 수 없습니다.") String> hobbyPurposes;

    @NotNull(message = "executionCount는 필수입니다.")
    @Min(value = 1, message = "executionCount는 1 이상이어야 합니다.")
    @Max(value = 7, message = "executionCount는 7 이하여야 합니다.")
    private Integer executionCount;

    @NotNull(message = "isDurationSet은 필수입니다.")
    private Boolean isDurationSet;
}
