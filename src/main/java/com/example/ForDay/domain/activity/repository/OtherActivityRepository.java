package com.example.ForDay.domain.activity.repository;

import com.example.ForDay.domain.activity.entity.OtherActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OtherActivityRepository extends JpaRepository<OtherActivity, Long> {
    @Query(value = "SELECT * FROM other_activities oa " +
            "WHERE oa.hobby_info_id = :hobbyInfoId " +
            "ORDER BY RAND() LIMIT 3", nativeQuery = true)
    List<OtherActivity> findRandomThreeByHobbyInfoId(@Param("hobbyInfoId") Long hobbyInfoId);
}
