package com.example.ForDay.domain.term.controller;

import com.example.ForDay.domain.term.dto.response.PrivacyTermsResponseDto;
import com.example.ForDay.domain.term.dto.response.ServiceTermsResponseDto;
import com.example.ForDay.domain.term.service.TermsService;
import com.example.ForDay.domain.term.type.DocumentType;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
