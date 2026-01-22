package com.example.ForDay.domain.activity.repository;

import com.example.ForDay.domain.activity.entity.ActivityRecord;
import com.example.ForDay.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityRecordRepository extends JpaRepository<ActivityRecord, Long>, ActivityRecordRepositoryCustom {
    long countByUserAndHobbyId(User currentUser, Long hobbyId);
}
