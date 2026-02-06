package com.example.ForDay.domain.term.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceTermsResponseDto {
    private String title;
    private String version;
    private List<SectionDto> sections;
    private ServiceInfoDto serviceInfo;

    @Getter
    @Builder
    public static class SectionDto {
        private Long sectionNo;
        private String sectionTitle;
        private List<ArticleDto> articles;
    }

    @Getter
    @Builder
    public static class ArticleDto {
        private Long articleId;
        private Integer clauseNo;
        private String content;
        private List<ItemDto> items;
    }

    @Getter
    @Builder
    public static class ItemDto {
        private Long itemId;
        private Integer itemNo;
        private String content;
    }

    @Getter
    @Builder
    public static class ServiceInfoDto {
        private String title;
        private String description;
        private String serviceName;
        private String companyName;
        private String email;
        private String representative;
        private String contactNumber;
    }
}