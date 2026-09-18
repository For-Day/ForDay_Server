package com.example.ForDay.domain.notification.repository;

import com.example.ForDay.domain.notification.dto.response.GetNotificationListResDto;
import com.example.ForDay.domain.notification.type.NotificationFilterType;
import com.example.ForDay.domain.user.entity.User;

public interface NotificationDocumentRepositoryCustom {

    GetNotificationListResDto getNotificationList(NotificationFilterType filterType, Long lastNotificationId, Integer pageSize, User currentUser);

    void updateImageUrlByRecordId(Long recordId, String newImageUrl);
}
