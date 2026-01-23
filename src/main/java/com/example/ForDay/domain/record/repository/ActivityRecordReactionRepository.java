package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.ActivityRecordReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityRecordReactionRepository extends JpaRepository <ActivityRecordReaction, Long>, ActivityRecordReactionRepositoryCustom {
    @Modifying
    @Query("UPDATE ActivityRecordReaction r SET r.readWriter = true WHERE r.activityRecord.id = :recordId")
    void markAsReadByRecordId(@Param("recordId") Long recordId);
}
