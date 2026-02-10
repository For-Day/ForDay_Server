package com.example.ForDay.domain.term.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "terms_articles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TermsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long articleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private TermsDocument termsDocument;

    @Column(nullable = false)
    private Long sectionNo; // 제 몇 조

    @Column(length = 100)
    private String sectionTitle; // 조항 제목

    private Integer clauseNo; // 제 몇 항 (없으면 NULL)

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 항 내용

    private Integer displayOrder; // UI 정렬용

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    @OneToMany(mappedBy = "termsArticle", cascade = CascadeType.ALL)
    private List<TermsArticleItem> items = new ArrayList<>();
}