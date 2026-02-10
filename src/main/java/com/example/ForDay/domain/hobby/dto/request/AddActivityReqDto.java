package com.example.ForDay.domain.hobby.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
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
    private List<IndividualActivityDto> activities;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "활동 생성 상세 정보") // 이 클래스에 대한 설명을 추가
    public static class IndividualActivityDto {

        @Schema(description = "AI 추천 여부", example = "true")
        @NotNull(message = "추천 여부는 필수입니다.")
        private boolean aiRecommended;

        @Schema(description = "활동 내용", example = "매일 아침 스트레칭 하기")
        @NotBlank(message = "activities content는 필수입니다.")
        @Size(max = 20, message = "activities content는 20자 이하여야 합니다.")
        private String content;

    }
}