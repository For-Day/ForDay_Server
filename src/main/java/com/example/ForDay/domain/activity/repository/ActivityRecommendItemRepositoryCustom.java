package com.example.ForDay.domain.activity.repository;

import com.example.ForDay.domain.activity.entity.ActivityRecommendItem;
import com.example.ForDay.domain.activity.type.AIItemType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityRecommendItemRepositoryCustom {

    List<ActivityRecommendItem> findAllByHobbyIdAndDate(Long id, LocalDateTime startOfToday, LocalDateTime endOfToday, AIItemType type);
}
