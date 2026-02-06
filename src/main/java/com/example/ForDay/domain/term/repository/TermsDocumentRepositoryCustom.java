package com.example.ForDay.domain.term.repository;

import com.example.ForDay.domain.term.entity.TermsDocument;
import com.example.ForDay.domain.term.type.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsDocumentRepositoryCustom{
    TermsDocument findLatestDocumentByType(DocumentType type);
}
