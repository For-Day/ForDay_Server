package com.example.ForDay.domain.activity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateActivityReqDto {

    @NotBlank(message = "활동 내용은 필수입니다.")
    @Size(min = 1, max = 100, message = "활동 내용은 1자 이상 100자 이하여야 합니다.")
    private String content;
}
