package com.example.ForDay.domain.hobby.dto.requ;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddActivityReqDto {
    @NotEmpty(message = "activities는 최소 1개 이상 필요합니다.")
    @Valid
    private List<ActivityDto> activities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityDto {

        @NotNull(message = "추천 여부는 필수입니다.")
        private boolean aiRecommended;

        @NotBlank(message = "activities content는 필수입니다.")
        @Size(max = 20, message = "activities content는 20자 이하여야 합니다.")
        private String content;
    }
}
