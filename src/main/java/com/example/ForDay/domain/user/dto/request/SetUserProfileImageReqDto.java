package com.example.ForDay.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetUserProfileImageReqDto {

    @Schema(description = "설정할 프로필 이미지 URL", example = "https://forday-s3.amazonaws.com/profiles/unique-image-name.png")
    @URL(message = "올바른 URL 형식이어야 합니다.")
    private String profileImageUrl;
}
