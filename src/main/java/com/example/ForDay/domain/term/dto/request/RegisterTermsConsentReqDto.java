package com.example.ForDay.domain.term.dto.request;

import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterTermsConsentReqDto {

    @AssertTrue(message = "서비스 이용약관에 동의해야 합니다.")
    private boolean serviceConsent;

    @AssertTrue(message = "14세 이상임을 동의해야 합니다.")
    private boolean ageOver14Consent;

    @AssertTrue(message = "개인정보 처리방침에 동의해야 합니다.")
    private boolean privateConsent;
    private boolean recordPushConsent;
}
