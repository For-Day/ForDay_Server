package com.example.ForDay.domain.notification.repository;

import com.example.ForDay.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationRepositoryCustom {

    @Modifying
    @Query("UPDATE Notification n SET n.imageUrl = :newImageUrl WHERE n.recordId = :recordId")
    void updateImageUrlByRecordId(@Param("recordId") Long recordId, @Param("newImageUrl") String newImageUrl);
}