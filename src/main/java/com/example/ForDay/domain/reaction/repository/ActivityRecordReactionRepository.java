package com.example.ForDay.domain.reaction.repository;

import com.example.ForDay.domain.record.dto.ReactionCountDto;
import com.example.ForDay.domain.record.dto.ReactionSummary;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.reaction.dto.ReactionKeyDto;
import com.example.ForDay.domain.reaction.entity.ActivityRecordReaction;
import com.example.ForDay.domain.record.type.RecordReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ActivityRecordReactionRepository extends JpaRepository <ActivityRecordReaction, Long>, ActivityRecordReactionRepositoryCustom {
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ActivityRecordReaction r SET r.readWriter = true " +
            "WHERE r.activityRecord.id = :recordId AND r.reactionType = :type")
    void markAsReadByRecordIdAndType(@Param("recordId") Long recordId, @Param("type") RecordReactionType type);

    @Query("SELECT new com.example.ForDay.domain.record.dto.ReactionSummary(" +
            "r.reactionType, r.reactedUser.id, r.readWriter) " +
            "FROM ActivityRecordReaction r WHERE r.activityRecord.id = :recordId")
    List<ReactionSummary> findReactionSummariesByRecordId(@Param("recordId") Long recordId);

    @Query("SELECT new com.example.ForDay.domain.record.dto.ReactionSummary(" +
            "r.reactionType, r.reactedUser.id, r.readWriter) " +
            "FROM ActivityRecordReaction r " +
            "WHERE r.activityRecord.id = :recordId " +
            "AND (r.reactedUser.id = :readerId OR r.readWriter = false)")
    List<ReactionSummary> findOptimizedSummaries(
            @Param("recordId") Long recordId,
            @Param("readerId") String readerId
    );

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

    @Modifying
    @Query("delete from ActivityRecordReaction r where r.activityRecord = :record")
    void deleteByActivityRecord(@Param("record") ActivityRecord record);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ActivityRecordReaction r SET r.readWriter = true " +
            "WHERE r.activityRecord.id = :recordId AND r.readWriter = false")
    void markAsReadByRecordId(@Param("recordId") Long recordId);

    // ReactionScheduler가 벌크 저장 전에 이미 존재하는 (recordId, userId, type) 조합을
    // 한 번의 쿼리로 걸러내기 위한 조회. recordId 단위로 미리 좁혀서 가져온 뒤
    // 정확한 (recordId, userId, type) 일치 여부는 자바 메모리에서 대조한다.
    @Query("SELECT new com.example.ForDay.domain.reaction.dto.ReactionKeyDto(" +
            "r.activityRecord.id, r.reactedUser.id, r.reactionType) " +
            "FROM ActivityRecordReaction r WHERE r.activityRecord.id IN :recordIds")
    List<ReactionKeyDto> findExistingKeysByRecordIds(@Param("recordIds") Collection<Long> recordIds);
}
