package com.example.ForDay.domain.activity.repository;

import com.example.ForDay.domain.activity.entity.ActivityRecommendItem;
import com.example.ForDay.domain.hobby.entity.Hobby;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityRecommendItemRepository extends JpaRepository<ActivityRecommendItem, Long> {
    @Query("SELECT a FROM ActivityRecommendItem a " +
            "JOIN FETCH a.hobby " +
            "WHERE a.hobby IN :hobbies " +
            "AND a.createdAt >= :start AND a.createdAt <= :end")
    List<ActivityRecommendItem> findAllByHobbiesAndDate(
            @Param("hobbies") List<Hobby> hobbies,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    @Modifying
    @Query("DELETE FROM ActivityRecommendItem a WHERE a.createdAt < :targetDate")
    void deleteOldItems(@Param("targetDate") LocalDateTime targetDate);
}
