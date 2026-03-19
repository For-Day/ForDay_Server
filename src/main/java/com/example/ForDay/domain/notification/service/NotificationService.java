package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.notification.dto.response.GetNotificationListResDto;
import com.example.ForDay.domain.notification.repository.NotificationRepository;
import com.example.ForDay.domain.notification.type.NotificationFilterType;
import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final UserUtil userUtil;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public GetNotificationListResDto getNotificationList(NotificationFilterType filterType, Long lastNotificationId, Integer pageSize, User user) {
        return notificationRepository.getNotificationList(filterType, lastNotificationId, pageSize, user);
    }
}
