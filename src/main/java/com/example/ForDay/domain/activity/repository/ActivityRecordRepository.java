package com.example.ForDay.domain.activity.repository;

import com.example.ForDay.domain.activity.entity.ActivityRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRecordRepository extends JpaRepository<ActivityRecord, Long>, ActivityRecordRepositoryCustom {
}
