package com.example.ForDay.domain.term.command;

/**
 * 약관 동의 값 묶음.
 *
 * <p>boolean 4개를 위치 인자로 넘기면 뒤바뀌어도 컴파일되므로 커맨드로 묶는다.
 */
public record TermsConsentCommand(
        boolean serviceConsent,
        boolean ageOver14Consent,
        boolean privateConsent,
        boolean recordPushConsent
) {
}
