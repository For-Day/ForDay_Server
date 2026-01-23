package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.ActivityRecordReaction;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ActivityRecordReactionRepository extends JpaRepository <ActivityRecordReaction, Long>, ActivityRecordReactionRepositoryCustom {
    @Modifying
    @Query("UPDATE ActivityRecordReaction r SET r.readWriter = true WHERE r.activityRecord.id = :recordId")
    void markAsReadByRecordId(@Param("recordId") Long recordId);

    Optional<ActivityRecordReaction> findByActivityRecordAndReactedUserAndReactionType(ActivityRecord activityRecord, User currentUser, RecordReactionType reactionType);
}
