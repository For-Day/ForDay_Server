package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.ActivityRecordReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ActivityRecordReportRepository extends JpaRepository<ActivityRecordReport, Long> {
    boolean existsByReportedRecordIdAndReporterId(Long recordId, String id);

    @Modifying
    @Query("delete from ActivityRecordReport r where r.reportedRecord = :record")
    void deleteByReportedRecord(@Param("record") ActivityRecord record);

    @Query("SELECT r.reportedRecord.id FROM ActivityRecordReport r WHERE r.reporter.id = :userId")
    List<Long> findReportedRecordIdsByReporterId(@Param("userId") String userId);
}
