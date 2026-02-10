package com.example.ForDay.domain.term.repository;

import com.example.ForDay.domain.term.entity.QTermsArticle;
import com.example.ForDay.domain.term.entity.QTermsArticleItem;
import com.example.ForDay.domain.term.entity.QTermsDocument;
import com.example.ForDay.domain.term.entity.TermsDocument;
import com.example.ForDay.domain.term.type.DocumentType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.JpaRepository;

@RequiredArgsConstructor
public class TermsDocumentRepositoryImpl implements TermsDocumentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public TermsDocument findLatestDocumentByType(DocumentType type) {
        QTermsDocument document = QTermsDocument.termsDocument;
        QTermsArticle article = QTermsArticle.termsArticle;
       // QTermsArticleItem item = QTermsArticleItem.termsArticleItem;

        return queryFactory
                .selectFrom(document)
                .leftJoin(document.articles, article).fetchJoin()
                .where(document.documentType.eq(type))
                .orderBy(
                        document.version.desc(),
                        article.sectionNo.asc(),
                        article.articleId.asc(),
                        article.clauseNo.asc().nullsFirst()
                )
                .fetchFirst();
    }}