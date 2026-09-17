package com.example.ForDay.domain.notification.type;

/**
 * {@code NotificationOutbox} 행의 발행 상태.
 *
 * <p>의도적으로 2단계만 둔다({@code attempts}/{@code FAILED} 없음) — v1은 무한 재시도로
 * 시작하고, 서킷브레이커는 실제로 필요해지면(포이즌 필 payload 등) 추가한다.
 */
public enum OutboxStatus {
    PENDING,
    PUBLISHED
}
