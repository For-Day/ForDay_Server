package com.example.ForDay.domain.notification.entity;

import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.domain.user.entity.User;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("COMMENT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentNotification extends Notification {
    private Long recordId;
    private Long commentId;
    private String commentContent;

    private CommentNotification(User receiver, User sender, NotificationType type, String message, Long recordId, Long commentId, String commentContent, String imageUrl) {
        super(receiver, sender, type, message, imageUrl);
        this.recordId = recordId;
        this.commentId = commentId;
        this.commentContent = commentContent;
    }

    public static CommentNotification create(User receiver, User sender, NotificationType type, String message, Long recordId, Long commentId, String commentContent, String imageUrl) {
        return new CommentNotification(receiver, sender, type, message, recordId, commentId, commentContent, imageUrl);
    }
}