package com.example.ForDay.domain.hobby.dto.request;

import com.example.ForDay.domain.record.type.RecordVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordActivityReqDto {

    @NotBlank(message = "스티커는 필수입니다.")
    private String sticker;

    @Size(max = 100, message = "메모는 최대 100자까지 입력 가능합니다.")
    private String memo;

    private String imageUrl;

    @NotNull(message = "공개 여부 설정은 필수입니다.")
    private RecordVisibility visibility;

}