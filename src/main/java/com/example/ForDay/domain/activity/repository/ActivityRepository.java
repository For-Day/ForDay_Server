package com.example.ForDay.domain.activity.repository;


import com.example.ForDay.domain.activity.entity.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ActivityRepository extends JpaRepository<Activity, Long>, ActivityRepositoryCustom{
    @Query("SELECT a FROM Activity a WHERE a.id = :activityId AND a.user.id = :userId")
    Optional<Activity> findByIdAndUserId(@Param("activityId") Long activityId,@Param("userId") String userId);
}
