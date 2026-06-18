package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.KeyboardKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KeyboardKeywordRepository extends JpaRepository<KeyboardKeyword, Long> {
    List<KeyboardKeyword> findAllByHobbyInfoId(Long hobbyInfoId);
}
