package com.example.ForDay.domain.notification.dto.response;

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
    }

    public static GetNotificationInfoResDto from(Notification n) {
        return GetNotificationInfoResDto.builder()
                .notificationId(n.getId())
                .message(n.getMessage())
                .type(n.getType())
                .imageUrl(n.getImageUrl())
                .read(n.isRead())
                .senderProfileUrl(n.getSender().getProfileImageUrl())
                .createdAt(TimeUtil.formatTimeAgo(n.getCreatedAt()))
                .build();
    }
}
