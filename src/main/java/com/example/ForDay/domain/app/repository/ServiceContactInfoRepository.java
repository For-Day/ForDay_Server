package com.example.ForDay.domain.app.repository;

import com.example.ForDay.domain.app.entity.ServiceContactInfo;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.security.Provider;
import java.util.Optional;

public interface ServiceContactInfoRepository extends JpaRepository<ServiceContactInfo, Long> {

    Optional<ServiceContactInfo> findFirstByOrderByInfoIdAsc();
}