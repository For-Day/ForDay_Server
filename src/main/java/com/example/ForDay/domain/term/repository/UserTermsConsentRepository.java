package com.example.ForDay.domain.term.repository;

import com.example.ForDay.domain.term.entity.UserTermsConsent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTermsConsentRepository extends JpaRepository<UserTermsConsent, Long> {
    boolean existsByUserId(String userId);
}
