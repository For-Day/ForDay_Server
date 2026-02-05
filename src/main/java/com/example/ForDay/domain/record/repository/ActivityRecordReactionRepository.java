package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.dto.ReactionCountDto;
import com.example.ForDay.domain.record.dto.ReactionSummary;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.ActivityRecordReaction;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ActivityRecordReactionRepository extends JpaRepository <ActivityRecordReaction, Long>, ActivityRecordReactionRepositoryCustom {
    @Modifying
    @Query("UPDATE ActivityRecordReaction r SET r.readWriter = true WHERE r.activityRecord.id = :recordId")
    void markAsReadByRecordId(@Param("recordId") Long recordId);

    @Modifying(clearAutomatically = true) // 영속성 컨텍스트 동기화
    @Query("UPDATE ActivityRecordReaction r SET r.readWriter = true " +
            "WHERE r.activityRecord.id = :recordId AND r.reactionType = :type")
    void markAsReadByRecordIdAndType(@Param("recordId") Long recordId, @Param("type") RecordReactionType type);

    Optional<ActivityRecordReaction> findByActivityRecordAndReactedUserAndReactionType(ActivityRecord activityRecord, User currentUser, RecordReactionType reactionType);

    boolean existsByActivityRecordAndReactedUserAndReactionType(ActivityRecord record, User testUser, RecordReactionType type);

    @Query("SELECT new com.example.ForDay.domain.record.dto.ReactionSummary(" +
            "r.reactionType, r.reactedUser.id, r.readWriter) " +
            "FROM ActivityRecordReaction r WHERE r.activityRecord.id = :recordId")
    List<ReactionSummary> findReactionSummariesByRecordId(@Param("recordId") Long recordId);

    @Modifying
    @Query("delete from ActivityRecordReaction r where r.activityRecord.id = :record")
    void deleteByActivityRecord(@Param("recordId") Long recordId);

    @Query("SELECT new com.example.ForDay.domain.record.dto.ReactionCountDto(r.activityRecord.id, COUNT(r)) " +
            "FROM ActivityRecordReaction r " +
            "WHERE r.createdAt >= :start AND r.createdAt <= :end " +
            "GROUP BY r.activityRecord.id")
    List<ReactionCountDto> countReactionsByDate(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(r) > 0 FROM ActivityRecordReaction r " +
            "WHERE r.activityRecord.id = :recordId " +
            "AND r.reactedUser.id = :userId " +
            "AND r.reactionType = :type")
    boolean existsByRecordIdAndUserIdAndType(
            @Param("recordId") Long recordId,
            @Param("userId") String userId,
            @Param("type") RecordReactionType type
    );

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ActivityRecordReaction r " +
            "WHERE r.activityRecord.id = :recordId " +
            "AND r.reactedUser.id = :userId " +
            "AND r.reactionType = :type")
    int deleteByRecordIdAndUserIdAndType(
            @Param("recordId") Long recordId,
            @Param("userId") String userId,
            @Param("type") RecordReactionType type
    );
}
