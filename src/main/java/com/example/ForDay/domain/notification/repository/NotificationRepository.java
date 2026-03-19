package com.example.ForDay.domain.notification.repository;

import com.example.ForDay.domain.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationRepositoryCustom {
}
