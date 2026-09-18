package com.example.ForDay.domain.notification.document;

import com.example.ForDay.domain.notification.type.NotificationType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

/**
 * 이슈 #406/#408 - 알림 도메인을 JPA {@code SINGLE_TABLE} 상속에서 MongoDB로 옮긴 document.
 *
 * <p>기존 구조는 알림 타입(REACTION/COMMENT)마다 컬럼이 부모 테이블(notifications)에 계속
 * 쌓이고, 조회 시 {@code instanceof} 분기로 타입을 나눠야 했다(개방-폐쇄 원칙 위반 - 새
 * 타입이 추가될 때마다 기존 분기 코드와 기존 테이블 스키마를 동시에 고쳐야 함). 여기서는
 * 공통 필드만 고정하고, 타입별로 달라지는 값은 {@link #payload}에 스키마 없이 담는다.
 * 새 알림 타입은 payload shape 하나만 새로 정의하면 되고, 기존 문서/조회 로직은 그대로 둔다.
 *
 * <p>{@code _id}는 Mongo 기본 {@code ObjectId}가 아니라 Long이다 - 기존 앱이 알림 ID를
 * 숫자로 다루는 API 계약을 유지하기 위해
 * {@link com.example.ForDay.global.mongo.service.MongoSequenceGeneratorService}로 직접
 * 발급한 값을 쓴다.
 *
 * <p>{@code senderProfileUrl}은 sender User를 조인하지 않고 알림 생성 시점 값을 그대로
 * 저장한다(비정규화) - RDB 버전이 목록 조회마다 LAZY sender 프록시를 초기화해 N+1을
 * 유발하던 문제도 함께 없어진다. 알림은 "그 시점의 스냅샷"을 보여주는 것이 UX상으로도
 * 더 맞다(이후 프로필 사진이 바뀌어도 과거 알림은 그대로 보임).
 */
@Document(collection = "notifications")
@CompoundIndex(name = "receiverId_id", def = "{'receiverId': 1, '_id': -1}")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDocument {

    public static final String SEQUENCE_NAME = "notifications_sequence";

    @Id
    private Long id;

    private String receiverId;
    private String senderId;
    private String senderProfileUrl;
    private NotificationType type;
    private String message;
    private boolean isRead;
    private String imageUrl;

    private Map<String, Object> payload;

    private LocalDateTime createdAt;

    @Builder
    private NotificationDocument(Long id, String receiverId, String senderId, String senderProfileUrl,
                                  NotificationType type, String message, String imageUrl,
                                  Map<String, Object> payload) {
        this.id = id;
        this.receiverId = receiverId;
        this.senderId = senderId;
        this.senderProfileUrl = senderProfileUrl;
        this.type = type;
        this.message = message;
        this.isRead = false;
        this.imageUrl = imageUrl;
        this.payload = payload;
        this.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
    }

    public static NotificationDocument create(Long id, String receiverId, String senderId, String senderProfileUrl,
                                               NotificationType type, String message, String imageUrl,
                                               Map<String, Object> payload) {
        return new NotificationDocument(id, receiverId, senderId, senderProfileUrl, type, message, imageUrl, payload);
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
