package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.ActivityRecordReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRecordReportRepository extends JpaRepository<ActivityRecordReport, Long> {
    boolean existsByReportedRecordIdAndReporterId(Long recordId, String id);
}
