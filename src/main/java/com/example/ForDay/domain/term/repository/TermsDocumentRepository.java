package com.example.ForDay.domain.term.repository;

import com.example.ForDay.domain.term.entity.TermsDocument;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermsDocumentRepository extends JpaRepository<TermsDocument, Long>, TermsDocumentRepositoryCustom {
}
