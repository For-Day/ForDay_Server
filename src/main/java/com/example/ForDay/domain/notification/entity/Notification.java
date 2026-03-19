package com.example.ForDay.domain.notification.entity;

import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Getter
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "dtype")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notifications")
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(nullable = false)
    private boolean isRead = false;

    protected Notification(User receiver, User sender, NotificationType type, String message) {
        this.receiver = receiver;
        this.sender = sender;
        this.type = type;
        this.message = message;
        this.isRead = false;
    }

    public static Notification create(User receiver, User sender, NotificationType type, String message) {
        return new Notification(receiver, sender, type, message);
    }

    public void markAsRead() {
        this.isRead = true;
    }
}