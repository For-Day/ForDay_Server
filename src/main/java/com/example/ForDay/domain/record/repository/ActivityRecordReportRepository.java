package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.ActivityRecordReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ActivityRecordReportRepository extends JpaRepository<ActivityRecordReport, Long> {
    @Query("SELECT COUNT(r) > 0 FROM ActivityRecordReport r " +
            "WHERE r.reportedRecord.id = :recordId " +
            "AND r.reporter.id = :reporterId")
    boolean existsByReportedRecordIdAndReporterId(@Param("recordId") Long recordId, @Param("reporterId") String reporterId);

    @Query("SELECT r.reportedRecord.id FROM ActivityRecordReport r WHERE r.reporter.id = :userId")
    List<Long> findReportedRecordIdsByReporterId(@Param("userId") String userId);

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ActivityRecordReport r WHERE r.reportedRecord.id = :recordId")
    void deleteByRecordId(@Param("recordId") Long recordId);

    @Modifying
    @Query("delete from ActivityRecordReport r where r.reportedRecord = :record")
    void deleteByReportedRecord(@Param("record") ActivityRecord record);
}
