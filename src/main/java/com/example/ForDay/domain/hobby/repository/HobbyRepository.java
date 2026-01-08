package com.example.ForDay.domain.hobby.repository;

import com.example.ForDay.domain.hobby.entity.Hobby;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HobbyRepository extends JpaRepository<Hobby, Long> {
}
