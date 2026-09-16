package com.example.ForDay.domain.notification.entity;

import com.example.ForDay.domain.notification.type.OutboxStatus;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Outbox 패턴의 발행 대기 행. {@code Notification} 저장과 같은 트랜잭션 안에서 함께 커밋되어,
 * "알림은 저장됐는데 발행 사실이 어디에도 안 남는" 상황을 막는다.
 *
 * <p>{@code NotificationOutboxRelay}가 주기적으로 PENDING 행을 읽어 RabbitMQ로 발행한다.
 * 발행 성공 행은 삭제하지 않고 {@link OutboxStatus#PUBLISHED}로 남겨 장애 디버깅 추적성을
 * 확보한다(정리 배치는 필요해지면 별도로 추가).
 *
 * <p>배경: {@code docs/adr/0002-notification-publish-after-commit.md}
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "notification_outbox")
public class NotificationOutbox extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_outbox_id")
    private Long id;

    // Notification.id 참조. FK 매핑이 아니라 평범한 Long이다 — 이 행의 유일한 책임은
    // "발행해야 할 일이 있다/없다"이지 Notification과의 연관관계 탐색이 아니다.
    @Column(nullable = false)
    private Long notificationId;

    // NotificationEventDto를 직렬화한 JSON. 릴레이가 이걸 그대로 역직렬화해 발행한다.
    @Lob
    @Column(nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OutboxStatus status;

    private LocalDateTime publishedAt;

    // 최초 실패 시각. null이면 아직 실패한 적이 없다 - Discord "최초 실패" edge-trigger 판단 기준.
    private LocalDateTime failedAt;

    private NotificationOutbox(Long notificationId, String payload) {
        this.notificationId = notificationId;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
    }

    public static NotificationOutbox pending(Long notificationId, String payload) {
        return new NotificationOutbox(notificationId, payload);
    }

    public boolean isPublished() {
        return status == OutboxStatus.PUBLISHED;
    }

    public boolean hadFailed() {
        return failedAt != null;
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    // 이미 실패한 적이 있으면 갱신하지 않는다 - failedAt은 "최초 실패 시각"이어야
    // edge-trigger(처음 실패할 때만 알림)가 성립한다.
    public void markFailed() {
        if (this.failedAt == null) {
            this.failedAt = LocalDateTime.now();
        }
    }
}
