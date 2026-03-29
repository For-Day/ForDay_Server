package com.example.ForDay.domain.notification.dto.response;

import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.domain.record.type.RecordReactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
    }

    // 알람의 종류가 COMMENT 일 때
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CommentAlramDto {
        private Long recordId;
        private Long commentId;
        private String commentContent;
    }
}
