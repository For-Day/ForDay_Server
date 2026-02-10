package com.example.ForDay.domain.term.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "terms_article_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TermsArticleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long itemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "article_id")
    private TermsArticle termsArticle;

    @Column(nullable = false)
    private Integer itemNo; // 항목 순번

    @Column(columnDefinition = "TEXT", nullable = false)
    private String itemContent; // 불릿 내용

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
