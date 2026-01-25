package com.example.ForDay.domain.hobby.repository;

import com.example.ForDay.domain.hobby.entity.HobbyCard;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HobbyCardRepository extends JpaRepository<HobbyCard, Long>, HobbyCardRepositoryCustom {
}
