package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.notification.document.NotificationDocument;
import com.example.ForDay.domain.notification.dto.response.GetNotificationListResDto;
import com.example.ForDay.domain.notification.repository.NotificationDocumentRepository;
import com.example.ForDay.domain.notification.type.NotificationFilterType;
import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.mongo.service.MongoSequenceGeneratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 이슈 #408 - {@link NotificationDocumentRepository}와 {@link MongoSequenceGeneratorService}를
 * 하나로 묶어 {@link NotificationService}가 두 협력자를 직접 알지 않게 한다. ArchUnit
 * S1(서비스 주입 의존성 8개 제한)을 넘기지 않기 위한 분리이기도 하다.
 */
@Service
@RequiredArgsConstructor
public class NotificationDocumentService {

    private final NotificationDocumentRepository notificationDocumentRepository;
    private final MongoSequenceGeneratorService sequenceGeneratorService;

    public Long save(String receiverId, String senderId, String senderProfileUrl, NotificationType type,
                      String message, String imageUrl, Map<String, Object> payload) {
        long id = sequenceGeneratorService.generateSequence(NotificationDocument.SEQUENCE_NAME);
        notificationDocumentRepository.save(
                NotificationDocument.create(id, receiverId, senderId, senderProfileUrl, type, message, imageUrl, payload));
        return id;
    }

    public void markAsReadIfUnread(Long notificationId) {
        if (notificationId == null) {
            return;
        }
        notificationDocumentRepository.findById(notificationId).ifPresent(document -> {
            document.markAsRead();
            notificationDocumentRepository.save(document);
        });
    }

    public boolean unreadNotificationExists(String receiverId) {
        return notificationDocumentRepository.existsByReceiverIdAndIsReadFalse(receiverId);
    }

    public GetNotificationListResDto getNotificationList(NotificationFilterType filterType, Long lastNotificationId, Integer pageSize, User currentUser) {
        return notificationDocumentRepository.getNotificationList(filterType, lastNotificationId, pageSize, currentUser);
    }
}
