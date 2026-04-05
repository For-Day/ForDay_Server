package com.example.ForDay.domain.term.dto.response;

import com.example.ForDay.global.common.response.message.UserSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterTermsConsentResDto {
    private String message;

    public static RegisterTermsConsentResDto of() {
        return new RegisterTermsConsentResDto(UserSuccessCode.AGREE_TERMS_SUCCESS.getMessage());
    }
}
