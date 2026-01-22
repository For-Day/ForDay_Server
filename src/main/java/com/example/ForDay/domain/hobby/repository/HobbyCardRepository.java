package com.example.ForDay.domain.hobby.repository;

import com.example.ForDay.domain.hobby.entity.HobbyInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HobbyCardRepository extends JpaRepository<HobbyInfo, Long> {
}
