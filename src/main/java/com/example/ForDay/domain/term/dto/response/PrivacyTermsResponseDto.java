package com.example.ForDay.domain.term.dto.response;

import com.example.ForDay.domain.app.entity.ServiceContactInfo;
import com.example.ForDay.domain.term.entity.TermsArticle;
import com.example.ForDay.domain.term.entity.TermsArticleItem;
import com.example.ForDay.domain.term.entity.TermsDocument;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PrivacyTermsResponseDto {
    private String title;
    private String description;
    private String version;
    private List<SectionDto> sections;
    private ServiceInfoDto serviceInfo;

    public static PrivacyTermsResponseDto of(TermsDocument doc, List<SectionDto> sections, ServiceContactInfo contact) {
        return PrivacyTermsResponseDto.builder()
                .title(doc.getTitle())
                .description(doc.getContent())
                .version(doc.getVersion())
                .sections(sections)
                .serviceInfo(ServiceInfoDto.from(contact))
                .build();
    }

    @Getter
    @Builder
    public static class SectionDto {
        private Long sectionNo;
        private String sectionTitle;
        private List<ArticleDto> articles;

        public static SectionDto of(Long sectionNo, String sectionTitle, List<ArticleDto> articles) {
            return SectionDto.builder()
                    .sectionNo(sectionNo)
                    .sectionTitle(sectionTitle)
                    .articles(articles)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ArticleDto {
        private Long articleId;
        private Integer clauseNo;
        private String content;
        private List<ItemDto> items;

        public static ArticleDto from(TermsArticle article) {
            return ArticleDto.builder()
                    .articleId(article.getArticleId())
                    .clauseNo(article.getClauseNo())
                    .content(article.getContent())
                    .items(article.getItems().stream()
                            .map(ItemDto::from)
                            .toList())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ItemDto {
        private Long itemId;
        private Integer itemNo;
        private String content;

        public static ItemDto from(TermsArticleItem item) {
            return ItemDto.builder()
                    .itemId(item.getItemId())
                    .itemNo(item.getItemNo())
                    .content(item.getItemContent())
                    .build();
        }
    }

    @Getter
    @Builder
    public static class ServiceInfoDto {
        private String serviceName;
        private String companyName;
        private String email;
        private String representative;
        private String contactNumber;

        public static ServiceInfoDto from(ServiceContactInfo info) {
            return ServiceInfoDto.builder()
                    .serviceName(info.getServiceName())
                    .companyName(info.getCompanyName())
                    .email(info.getEmail())
                    .representative(info.getRepresentative())
                    .contactNumber(info.getContactNumber())
                    .build();
        }
    }
}