package com.example.ForDay.domain.term.entity;

import com.example.ForDay.domain.term.dto.request.RegisterTermsConsentReqDto;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_terms_consent")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTermsConsent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private boolean serviceConsent;

    @Column(nullable = false)
    private boolean ageOver14Consent;

    @Column(nullable = false)
    private boolean privateConsent;

    @Column(nullable = false)
    private boolean recordPushConsent;

    // 동의한 약관 버전
    @Column(nullable = false, length = 20)
    private String agreementVersion;

    public static UserTermsConsent create(RegisterTermsConsentReqDto reqDto, String userId) {
        return UserTermsConsent
                .builder()
                .userId(userId)
                .serviceConsent(reqDto.isServiceConsent())
                .ageOver14Consent(reqDto.isAgeOver14Consent())
                .privateConsent(reqDto.isPrivateConsent())
                .recordPushConsent(reqDto.isRecordPushConsent())
                .agreementVersion("1.0.0")
                .build();
    }
}