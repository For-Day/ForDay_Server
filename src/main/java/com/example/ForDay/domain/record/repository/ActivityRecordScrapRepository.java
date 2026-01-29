package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.ActivityRecordScarp;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRecordScrapRepository extends JpaRepository<ActivityRecordScarp, Long> {
    boolean existsByActivityRecordIdAndUserId(Long activityRecordId, String id);
}
