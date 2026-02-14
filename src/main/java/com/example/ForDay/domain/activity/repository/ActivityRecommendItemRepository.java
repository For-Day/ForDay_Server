package com.example.ForDay.domain.activity.repository;

import com.example.ForDay.domain.activity.entity.ActivityRecommendItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityRecommendItemRepository extends JpaRepository<ActivityRecommendItem, Long> {
    @Modifying
    @Query("DELETE FROM ActivityRecommendItem a WHERE a.createdAt < :targetDate")
    void deleteOldItems(@Param("targetDate") LocalDateTime targetDate);

    @Query("SELECT a FROM ActivityRecommendItem a " +
            "JOIN FETCH a.hobby h " +
            "WHERE h.id = :hobbyId " +
            "AND a.createdAt BETWEEN :start AND :end")
    List<ActivityRecommendItem> findAllByHobbyIdAndDate(
            @Param("hobbyId") Long hobbyId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}
