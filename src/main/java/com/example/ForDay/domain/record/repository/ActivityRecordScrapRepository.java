package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.ActivityRecordScarp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActivityRecordScrapRepository extends JpaRepository<ActivityRecordScarp, Long> {
    boolean existsByActivityRecordIdAndUserId(Long activityRecordId, String id);

    Optional<ActivityRecordScarp> findByActivityRecordIdAndUserId(Long id, String id1);
}
