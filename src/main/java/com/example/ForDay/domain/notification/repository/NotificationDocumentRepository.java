package com.example.ForDay.domain.notification.repository;

import com.example.ForDay.domain.notification.document.NotificationDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface NotificationDocumentRepository extends MongoRepository<NotificationDocument, Long>, NotificationDocumentRepositoryCustom {

    boolean existsByReceiverIdAndIsReadFalse(String receiverId);

    long countByReceiverId(String receiverId);

    void deleteByReceiverId(String receiverId);
}
