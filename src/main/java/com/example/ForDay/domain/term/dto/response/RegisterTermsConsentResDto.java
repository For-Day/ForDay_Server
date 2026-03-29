package com.example.ForDay.domain.term.dto.response;

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
        return new RegisterTermsConsentResDto("약관 동의가 정상적으로 수집되었습니다.");
    }
}
