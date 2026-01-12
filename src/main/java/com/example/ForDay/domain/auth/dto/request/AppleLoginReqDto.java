package com.example.ForDay.domain.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppleLoginReqDto {

    @NotBlank(message = "Apple authorization code는 필수입니다.")
    private String code;
}
