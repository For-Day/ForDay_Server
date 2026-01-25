package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.UserRecordCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRecordCountRepository extends JpaRepository<UserRecordCount, String> {

    @Modifying
    @Query("UPDATE UserRecordCount u SET u.recordCount = u.recordCount + 1 WHERE u.id = :userId")
    int incrementRecordCount(@Param("userId") String userId);
}
