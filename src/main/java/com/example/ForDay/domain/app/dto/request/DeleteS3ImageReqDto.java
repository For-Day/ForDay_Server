package com.example.ForDay.domain.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteS3ImageReqDto {
    @NotBlank(message = "이미지 URL은 필수입니다.")
    @Pattern(
            regexp = "^(https?://).+",
            message = "올바른 이미지 URL 형식이 아닙니다."
    )
    private String imageUrl;
}
