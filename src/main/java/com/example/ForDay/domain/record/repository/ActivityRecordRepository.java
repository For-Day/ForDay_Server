package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.dto.RecordDeleteCheckDto;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.user.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface ActivityRecordRepository extends JpaRepository<ActivityRecord, Long>, ActivityRecordRepositoryCustom {
    long countByUserAndHobbyId(User currentUser, Long hobbyId);

    @Query("SELECT COUNT(ar) FROM ActivityRecord ar " +
            "WHERE ar.user.id = :userId AND ar.hobby.id = :hobbyId")
    long countByUserIdAndHobbyId(@Param("userId") String userId, @Param("hobbyId") Long hobbyId);

    @Query("select ar from ActivityRecord ar " +
            "join fetch ar.user " +
            "join fetch ar.activity " +
            "where ar.id = :recordId")
    Optional<ActivityRecord> findByIdWithUserAndActivity(@Param("recordId") Long recordId);

    @Query("SELECT ar FROM ActivityRecord ar " +
            "JOIN FETCH ar.hobby " +
            "JOIN FETCH ar.user " +
            "WHERE ar.id = :recordId")
    Optional<ActivityRecord> findByIdWithHobby(@Param("recordId") Long recordId);

    Optional<ActivityRecord> findByIdAndUserId(Long recordId, String currentUserId);

    @Query("SELECT ar FROM ActivityRecord ar " +
            "JOIN FETCH ar.activity " +
            "JOIN FETCH ar.hobby " +
            "WHERE ar.id = :recordId AND ar.user.id = :userId")
    Optional<ActivityRecord> findByIdWithDetails(@Param("recordId") Long recordId, @Param("userId") String userId);

    long countByUserIdAndHobbyIdAndCreatedAtAfterAndDeletedFalse(String userId, Long hobbyId, LocalDateTime sevenDaysAgo);

    @Query("SELECT ar FROM ActivityRecord ar " +
            "WHERE ar.hobby.id = :hobbyId " +
            "AND ar.imageUrl IS NOT NULL " +
            "AND ar.imageUrl <> '' " +
            "AND ar.deleted = false " +
            "ORDER BY ar.createdAt DESC")
    Optional<ActivityRecord> findLatestImageRecord(@Param("hobbyId") Long hobbyId);


    @Query("SELECT new com.example.ForDay.domain.record.dto.RecordDeleteCheckDto(" +
            "r.createdAt, r.deleted, r.imageUrl, r.hobby.id) " +
            "FROM ActivityRecord r " +
            "WHERE r.id = :recordId AND r.user.id = :userId")
    Optional<RecordDeleteCheckDto> findDeleteCheckDto(
            @Param("recordId") Long recordId,
            @Param("userId") String userId
    );

    @Modifying
    @Query("UPDATE ActivityRecord r SET r.deleted = true, r.updatedAt = NOW() WHERE r.id = :recordId")
    void softDeleteRecord(Long recordId);
}
