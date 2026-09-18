package com.example.ForDay.domain.notification.dto.response;

import com.example.ForDay.domain.notification.document.NotificationDocument;
import com.example.ForDay.domain.notification.entity.CommentNotification;
import com.example.ForDay.domain.notification.entity.Notification;
import com.example.ForDay.domain.notification.entity.ReactionNotification;
import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.global.util.TimeUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetNotificationInfoResDto {
    private Long notificationId;
    private String imageUrl;
    private String message;
    private NotificationType type;
    private ReactionAlramDto reactionAlram;
    private CommentAlramDto commentAlram;
    private boolean read;
    private String senderProfileUrl;
    private String createdAt;

    // 알람의 종류가 RECORD 일 때
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ReactionAlramDto {
        private RecordReactionType reactionType;
        private Long recordId;

        public static ReactionAlramDto from(ReactionNotification reactionNotification) {
            return new ReactionAlramDto(
                    reactionNotification.getReactionType(),
                    reactionNotification.getRecordId()
            );
        }

        // Mongo payload는 스키마리스 Map<String,Object>라 타입을 보장하지 않는다 - 저장 시점에
        // reactionType은 항상 enum.name()(String), recordId는 항상 Long으로 넣지만(NotificationService
        // 참고), 읽을 때는 방어적으로 캐스팅한다.
        public static ReactionAlramDto fromPayload(Map<String, Object> payload) {
            return new ReactionAlramDto(
                    RecordReactionType.valueOf((String) payload.get("reactionType")),
                    toLong(payload.get("recordId"))
            );
        }
    }

    // 알람의 종류가 COMMENT 일 때
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CommentAlramDto {
        private Long recordId;
        private Long commentId;
        private String commentContent;

        public static CommentAlramDto from(CommentNotification commentNotification) {
            return new CommentAlramDto(
                    commentNotification.getRecordId(),
                    commentNotification.getCommentId(),
                    commentNotification.getCommentContent()
            );
        }

        // CommentNotification과 마찬가지로 실제 생성 지점은 아직 없다(Comment 도메인
        // 부재). 알림 타입이 늘어나도 조회 로직을 고치지 않는다는 걸 보이기 위해 구조만
        // 미리 맞춰둔다.
        public static CommentAlramDto fromPayload(Map<String, Object> payload) {
            return new CommentAlramDto(
                    toLong(payload.get("recordId")),
                    toLong(payload.get("commentId")),
                    (String) payload.get("commentContent")
            );
        }
    }

    public static GetNotificationInfoResDto from(Notification n) {
        return GetNotificationInfoResDto.builder()
                .notificationId(n.getId())
                .message(n.getMessage())
                .type(n.getType())
                .imageUrl(n.getImageUrl())
                .read(n.isRead())
                .senderProfileUrl(n.getSender() != null ? n.getSender().getProfileImageUrl() : null)
                .createdAt(TimeUtil.formatTimeAgo(n.getCreatedAt()))
                .build();
    }

    // MongoDB 알림 도메인(#406/#408)의 실제 조회 경로. senderProfileUrl은 위 JPA
    // 버전처럼 sender를 조인해 가져오지 않고, 알림 생성 시점에 이미 저장해둔 값을 그대로
    // 쓴다(NotificationDocument 클래스 Javadoc 참고) - N+1도 함께 사라진다.
    public static GetNotificationInfoResDto from(NotificationDocument n) {
        return GetNotificationInfoResDto.builder()
                .notificationId(n.getId())
                .message(n.getMessage())
                .type(n.getType())
                .imageUrl(n.getImageUrl())
                .read(n.isRead())
                .senderProfileUrl(n.getSenderProfileUrl())
                .createdAt(TimeUtil.formatTimeAgo(n.getCreatedAt()))
                .build();
    }

    private static Long toLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }
}
