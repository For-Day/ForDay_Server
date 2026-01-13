package com.example.ForDay.domain.hobby.dto.request;

import com.example.ForDay.domain.activity.type.RecordVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordActivityReqDto {

    @NotBlank(message = "스티커는 필수입니다.")
    private String sticker;

    @Size(max = 100, message = "메모는 최대 100자까지 입력 가능합니다.")
    private String memo;

    @Size(max = 3, message = "이미지는 최대 3개까지 등록 가능합니다.")
    @Valid // 내부 객체인 ImageDto의 유효성 검사도 수행하기 위해 필요
    private List<ImageDto> images;

    @NotNull(message = "공개 여부 설정은 필수입니다.")
    private RecordVisibility visibility;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ImageDto {
        @NotNull(message = "이미지 순서는 필수입니다.")
        private Integer order;

        @NotBlank(message = "이미지 URL은 필수입니다.")
        private String imageUrl;
    }
}