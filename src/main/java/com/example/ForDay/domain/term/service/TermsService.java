package com.example.ForDay.domain.term.service;

import com.example.ForDay.domain.app.entity.ServiceContactInfo;
import com.example.ForDay.domain.app.repository.ServiceContactInfoRepository;
import com.example.ForDay.domain.term.dto.response.PrivacyTermsResponseDto;
import com.example.ForDay.domain.term.dto.response.ServiceTermsResponseDto;
import com.example.ForDay.domain.term.entity.TermsArticle;
import com.example.ForDay.domain.term.entity.TermsDocument;
import com.example.ForDay.domain.term.repository.TermsDocumentRepository;
import com.example.ForDay.domain.term.type.DocumentType;
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
                .map(entry -> {
                    List<TermsArticle> articles = entry.getValue();
                    return ServiceTermsResponseDto.SectionDto.builder()
                            .sectionNo(entry.getKey())
                            .sectionTitle(articles.get(0).getSectionTitle())
                            .articles(articles.stream().map(this::convertToServiceArticleDto).toList())
                            .build();
                }).toList();

        ServiceContactInfo serviceContactInfo = serviceContactInfoRepository.findFirstByOrderByInfoIdAsc()
                .orElseThrow(() -> new RuntimeException("서비스 연락처 정보를 찾을 수 없습니다."));

        return ServiceTermsResponseDto.builder()
                .title(document.getTitle())
                .version(document.getVersion())
                .sections(sectionDtos)
                .serviceInfo(ServiceTermsResponseDto.ServiceInfoDto.builder()
                        .title("부칙")
                        .description("본 약관은 [2026-02-07]부터 시행됩니다.")
                        .serviceName(serviceContactInfo.getServiceName())
                        .companyName(serviceContactInfo.getCompanyName())
                        .email(serviceContactInfo.getEmail())
                        .representative(serviceContactInfo.getRepresentative())
                        .contactNumber(serviceContactInfo.getContactNumber())
                        .build())
                .build();
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
                .map(entry -> {
                    List<TermsArticle> articles = entry.getValue();
                    return PrivacyTermsResponseDto.SectionDto.builder()
                            .sectionNo(entry.getKey())
                            .sectionTitle(articles.get(0).getSectionTitle())
                            .articles(articles.stream().map(this::convertToPrivacyArticleDto).toList())
                            .build();
                }).toList();

        ServiceContactInfo serviceContactInfo = serviceContactInfoRepository.findFirstByOrderByInfoIdAsc()
                .orElseThrow(() -> new RuntimeException("서비스 연락처 정보를 찾을 수 없습니다."));

        return PrivacyTermsResponseDto.builder()
                .title(document.getTitle())
                .description(document.getContent())
                .version(document.getVersion())
                .sections(sectionDtos)
                .serviceInfo(PrivacyTermsResponseDto.ServiceInfoDto.builder()
                        .serviceName(serviceContactInfo.getServiceName())
                        .companyName(serviceContactInfo.getCompanyName())
                        .email(serviceContactInfo.getEmail())
                        .representative(serviceContactInfo.getRepresentative())
                        .contactNumber(serviceContactInfo.getContactNumber())
                        .build())
                .build();
    }

    // =========================
    // ✅ 변환 메서드 분리
    // =========================

    private ServiceTermsResponseDto.ArticleDto convertToServiceArticleDto(TermsArticle article) {
        return ServiceTermsResponseDto.ArticleDto.builder()
                .articleId(article.getArticleId())
                .clauseNo(article.getClauseNo())
                .content(article.getContent())
                .items(article.getItems().stream()
                        .map(i -> ServiceTermsResponseDto.ItemDto.builder()
                                .itemId(i.getItemId())
                                .itemNo(i.getItemNo())
                                .content(i.getItemContent())
                                .build())
                        .toList())
                .build();
    }

    private PrivacyTermsResponseDto.ArticleDto convertToPrivacyArticleDto(TermsArticle article) {
        return PrivacyTermsResponseDto.ArticleDto.builder()
                .articleId(article.getArticleId())
                .clauseNo(article.getClauseNo())
                .content(article.getContent())
                .items(article.getItems().stream()
                        .map(i -> PrivacyTermsResponseDto.ItemDto.builder()
                                .itemId(i.getItemId())
                                .itemNo(i.getItemNo())
                                .content(i.getItemContent())
                                .build())
                        .toList())
                .build();
    }
}
