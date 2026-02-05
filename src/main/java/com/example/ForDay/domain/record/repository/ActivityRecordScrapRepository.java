package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.ActivityRecordScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ActivityRecordScrapRepository extends JpaRepository<ActivityRecordScrap, Long>, ActivityRecordScrapRepositoryCustom {
    boolean existsByActivityRecordIdAndUserId(Long activityRecordId, String id);

    Optional<ActivityRecordScrap> findByActivityRecordIdAndUserId(Long id, String id1);

    long countByUserId(String id);

    @Modifying(clearAutomatically = true)
    @Query("delete from ActivityRecordScrap s where s.activityRecord.id = :recordId")
    void deleteByActivityRecord(@Param("recordId") Long recordId);

    @Query("SELECT COUNT(s) > 0 FROM ActivityRecordScrap s " +
            "WHERE s.activityRecord.id = :recordId " +
            "AND s.user.id = :userId")
    boolean existsByScrap(
            @Param("recordId") Long recordId,
            @Param("userId") String userId
    );
}
