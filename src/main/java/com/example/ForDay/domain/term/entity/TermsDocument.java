package com.example.ForDay.domain.term.entity;

import com.example.ForDay.domain.term.type.DocumentType;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "terms_documents")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TermsDocument extends BaseTimeEntity{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long documentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DocumentType documentType;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(nullable = false)
    private Boolean isMandatory; // 필수 여부

    private LocalDateTime effectiveAt; // 시행일

    @Builder.Default
    @OneToMany(mappedBy = "termsDocument", cascade = CascadeType.ALL)
    private List<TermsArticle> articles = new ArrayList<>();
}
