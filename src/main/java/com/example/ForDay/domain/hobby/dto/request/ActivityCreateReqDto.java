package com.example.ForDay.domain.hobby.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityCreateReqDto {

    private Long hobbyCardId;

    @NotBlank(message = "hobbyName은 필수입니다.")
    private String hobbyName;

    @NotNull(message = "hobbyTimeMinutes는 필수입니다.")
    @Min(value = 1, message = "hobbyTimeMinutes는 1분 이상이어야 합니다.")
    private Integer hobbyTimeMinutes;

    @NotEmpty(message = "hobbyPurposes는 최소 1개 이상 필요합니다.")
    private List<@NotBlank(message = "취미 목적은 빈 값일 수 없습니다.") String> hobbyPurposes;

    @NotNull(message = "executionCount는 필수입니다.")
    @Min(value = 1, message = "executionCount는 1 이상이어야 합니다.")
    private Integer executionCount;

    @NotNull(message = "isDurationSet은 필수입니다.")
    private Boolean isDurationSet;

    @NotEmpty(message = "activities는 최소 1개 이상 필요합니다.")
    @Valid
    private List<ActivityDto> activities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityDto {

        @NotBlank(message = "aiRecommended는 필수 값입니다.")
        private boolean aiRecommended;

        @NotBlank(message = "activities content는 필수입니다.")
        @Size(max = 100, message = "activities content는 100자 이하여야 합니다.")
        private String content;

        @Size(max = 255, message = "activities description은 255자 이하여야 합니다.")
        private String description;
    }
}
