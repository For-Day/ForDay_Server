package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.ActivityRecordScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ActivityRecordScrapRepository extends JpaRepository<ActivityRecordScrap, Long>, ActivityRecordScrapRepositoryCustom {
    Optional<ActivityRecordScrap> findByActivityRecordIdAndUserId(Long id, String id1);

    @Query("SELECT COUNT(s) FROM ActivityRecordScrap s WHERE s.user.id = :userId")
    long countByUserId(@Param("userId") String userId);

    @Query("SELECT COUNT(s) > 0 FROM ActivityRecordScrap s " +
            "WHERE s.activityRecord.id = :recordId " +
            "AND s.user.id = :userId")
    boolean existsByScrap(
            @Param("recordId") Long recordId,
            @Param("userId") String userId
    );

    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM ActivityRecordScrap s WHERE s.activityRecord.id = :recordId")
    void deleteByRecordId(@Param("recordId") Long recordId);

    @Modifying
    @Query("delete from ActivityRecordScrap s where s.activityRecord = :activityRecord")
    void deleteByActivityRecord(@Param("activityRecord") ActivityRecord activityRecord);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ActivityRecordScrap s where s.activityRecord.id = :recordId")
    void deleteByActivityRecordId(@Param("recordId") Long recordId);

}
