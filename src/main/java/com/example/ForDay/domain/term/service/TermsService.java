package com.example.ForDay.domain.term.service;

import com.example.ForDay.domain.app.entity.ServiceContactInfo;
import com.example.ForDay.domain.app.repository.ServiceContactInfoRepository;
import com.example.ForDay.domain.term.dto.request.RegisterTermsConsentReqDto;
import com.example.ForDay.domain.term.dto.response.PrivacyTermsResponseDto;
import com.example.ForDay.domain.term.dto.response.RegisterTermsConsentResDto;
import com.example.ForDay.domain.term.dto.response.ServiceTermsResponseDto;
import com.example.ForDay.domain.term.entity.TermsArticle;
import com.example.ForDay.domain.term.entity.TermsDocument;
import com.example.ForDay.domain.term.entity.UserTermsConsent;
import com.example.ForDay.domain.term.repository.TermsDocumentRepository;
import com.example.ForDay.domain.term.repository.UserTermsConsentRepository;
import com.example.ForDay.domain.term.type.DocumentType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.firebase.entity.FcmToken;
import com.example.ForDay.global.firebase.service.FcmTokenService;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsService {

    private final TermsDocumentRepository termsRepository;
    private final ServiceContactInfoRepository serviceContactInfoRepository;
    private final UserUtil userUtil;
    private final UserTermsConsentRepository userTermsConsentRepository;
    private final FcmTokenService fcmTokenService;

    public ServiceTermsResponseDto getServiceTerms(DocumentType type) {
        TermsDocument document = termsRepository.findLatestDocumentByType(type);
        if (document == null) throw new RuntimeException("해당 약관을 찾을 수 없습니다.");

        Map<Long, List<TermsArticle>> groupedBySection = document.getArticles().stream()
                .collect(Collectors.groupingBy(
                        TermsArticle::getSectionNo,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<ServiceTermsResponseDto.SectionDto> sectionDtos = groupedBySection.entrySet().stream()
                .map(entry -> ServiceTermsResponseDto.SectionDto.of(
                        entry.getKey(),
                        entry.getValue().get(0).getSectionTitle(),
                        entry.getValue().stream().map(ServiceTermsResponseDto.ArticleDto::from).toList()
                )).toList();

        ServiceContactInfo serviceContactInfo = serviceContactInfoRepository.findFirstByOrderByInfoIdAsc()
                .orElseThrow(() -> new RuntimeException("서비스 연락처 정보를 찾을 수 없습니다."));

        return ServiceTermsResponseDto.of(document, sectionDtos, serviceContactInfo);
    }

    public PrivacyTermsResponseDto getPrivacyTerms(DocumentType type) {
        TermsDocument document = termsRepository.findLatestDocumentByType(type);
        if (document == null) throw new RuntimeException("해당 약관을 찾을 수 없습니다.");

        Map<Long, List<TermsArticle>> groupedBySection = document.getArticles().stream()
                .collect(Collectors.groupingBy(
                        TermsArticle::getSectionNo,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<PrivacyTermsResponseDto.SectionDto> sectionDtos = groupedBySection.entrySet().stream()
                .map(entry -> PrivacyTermsResponseDto.SectionDto.of(
                        entry.getKey(),
                        entry.getValue().get(0).getSectionTitle(),
                        entry.getValue().stream()
                                .map(PrivacyTermsResponseDto.ArticleDto::from) // Static Factory Method 활용
                                .toList()
                )).toList();

        ServiceContactInfo serviceContactInfo = serviceContactInfoRepository.findFirstByOrderByInfoIdAsc()
                .orElseThrow(() -> new RuntimeException("서비스 연락처 정보를 찾을 수 없습니다."));

        return PrivacyTermsResponseDto.of(document, sectionDtos, serviceContactInfo);
    }

    @Transactional
    public RegisterTermsConsentResDto registerTermsConsent(RegisterTermsConsentReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        if(existsTermsConsent(currentUser)) {
            throw new CustomException(ErrorCode.TERMS_CONSENT_ALREADY_EXISTS);
        }

        userTermsConsentRepository.save(UserTermsConsent.create(reqDto, currentUser.getId()));

        if(reqDto.isRecordPushConsent()) {
            setUserRecordPushEnabled(currentUser.getId());
        }
        currentUser.completeTermsConsent();

        return RegisterTermsConsentResDto.of();
    }

    private void setUserRecordPushEnabled(String userId) {
        List<FcmToken> fcmTokenList = fcmTokenService.findUserFcmToken(userId);

        fcmTokenList.forEach(fcmToken -> fcmToken.updateRecordPushEnabled(true));
    }

    private boolean existsTermsConsent(User currentUser) {
        return userTermsConsentRepository.existsByUserId(currentUser.getId());
    }
}
