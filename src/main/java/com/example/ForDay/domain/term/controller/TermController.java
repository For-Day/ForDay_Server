package com.example.ForDay.domain.term.controller;

import com.example.ForDay.domain.term.dto.request.RegisterTermsConsentReqDto;
import com.example.ForDay.domain.term.dto.response.PrivacyTermsResponseDto;
import com.example.ForDay.domain.term.dto.response.RegisterTermsConsentResDto;
import com.example.ForDay.domain.term.dto.response.ServiceTermsResponseDto;
import com.example.ForDay.domain.term.service.TermsService;
import com.example.ForDay.domain.term.type.DocumentType;
import com.example.ForDay.global.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/terms")
@RequiredArgsConstructor
public class TermController {
    private final TermsService termsService;

    @GetMapping("/service")
    public ServiceTermsResponseDto getServiceTerms() {
        return termsService.getServiceTerms(DocumentType.TERMS);
    }

    @GetMapping("/privacy")
    public PrivacyTermsResponseDto getPrivacyPolicy() {
        return termsService.getPrivacyTerms(DocumentType.PRIVACY);
    }

    @PostMapping("/consent")
    public RegisterTermsConsentResDto registerTermsConsent(@RequestBody @Valid RegisterTermsConsentReqDto reqDto,
                                                           @AuthenticationPrincipal CustomUserDetails user) {
        return termsService.registerTermsConsent(reqDto, user);
    }
}
