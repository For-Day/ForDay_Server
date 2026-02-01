package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.ActivityRecordScarp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ActivityRecordScrapRepository extends JpaRepository<ActivityRecordScarp, Long>, ActivityRecordScrapRepositoryCustom {
    boolean existsByActivityRecordIdAndUserId(Long activityRecordId, String id);

    Optional<ActivityRecordScarp> findByActivityRecordIdAndUserId(Long id, String id1);

    long countByUserId(String id);

    @Modifying
    @Query("delete from ActivityRecordScarp s where s.activityRecord = :activityRecord")
    void deleteByActivityRecord(@Param("activityRecord") ActivityRecord activityRecord);
}
