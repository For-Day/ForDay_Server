package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ActivityRecordRepository extends JpaRepository<ActivityRecord, Long>, ActivityRecordRepositoryCustom {
    long countByUserAndHobbyId(User currentUser, Long hobbyId);

    @Query("select ar from ActivityRecord ar " +
            "join fetch ar.user " +
            "join fetch ar.activity " +
            "where ar.id = :recordId")
    Optional<ActivityRecord> findByIdWithUserAndActivity(@Param("recordId") Long recordId);
}
