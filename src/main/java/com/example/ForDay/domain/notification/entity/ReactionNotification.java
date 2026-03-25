package com.example.ForDay.domain.notification.entity;

import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.User;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("REACTION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReactionNotification extends Notification {
    private RecordReactionType reactionType;
    private Long recordId;

    private ReactionNotification(User receiver, User sender, NotificationType type, String message, RecordReactionType reactionType, Long recordId) {
        super(receiver, sender, type, message); // 부모 생성자 호출
        this.reactionType = reactionType;
        this.recordId = recordId;
    }

    public static ReactionNotification create(User receiver, User sender, NotificationType type, String message, RecordReactionType reactionType, Long recordId) {
        return new ReactionNotification(receiver, sender, type, message, reactionType, recordId);
    }
}
