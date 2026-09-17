package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.notification.entity.NotificationOutbox;
import com.example.ForDay.domain.notification.repository.NotificationOutboxRepository;
import com.example.ForDay.global.port.DiscordAlertPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * {@link NotificationOutbox}의 PENDING 행을 주기적으로 읽어 발행을 트리거하는 릴레이.
 *
 * <p>{@code ReactionScheduler}(Redis 큐 배수)와 같은 성격의 "신뢰 가능한 큐 폴링" 패턴을
 * SQL 테이블에 다시 적용한 것이다. 실제 클레임+발행은 {@link NotificationOutboxItemPublisher}가
 * 건별 트랜잭션으로 처리한다(자기 자신 호출로는 {@code @Transactional}이 적용되지 않아
 * 분리했다 - 그쪽 클래스 Javadoc 참고).
 *
 * <p>배경: {@code docs/adr/0002-notification-publish-after-commit.md}
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationOutboxRelay {

    private static final int BATCH_SIZE = 100;

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationOutboxItemPublisher itemPublisher;
    private final DiscordAlertPort discordAlertPort;

    @Scheduled(fixedDelay = 1000)
    public void relay() {
        List<Long> pendingIds = outboxRepository.findPendingIds(PageRequest.of(0, BATCH_SIZE));

        // 건별로 독립 트랜잭션을 잡는다(itemPublisher 안에서) - 한 건의 발행 실패나
        // 락 대기가 나머지 건의 발행을 막지 않게 하기 위해서다.
        for (Long id : pendingIds) {
            switch (itemPublisher.publish(id)) {
                case FIRST_FAILURE -> alert(() -> discordAlertPort.send(failureMessage(id)));
                case RECOVERED -> alert(() -> discordAlertPort.send(recoveredMessage(id)));
                case NOOP -> { /* 반복 실패거나 이미 처리됨 - 알릴 것 없음 */ }
            }
        }
    }

    // Discord 호출은 outbox 행의 락이 풀린 뒤, 트랜잭션 밖에서 한다 - 느린 외부 I/O를
    // DB 락과 묶어두지 않기 위해서다(#371에서 FCM을 트랜잭션 밖으로 뺀 것과 같은 이유).
    // 실패해도 릴레이 자체는 계속 돌아야 하므로 여기서 감싼다.
    private void alert(Runnable alertAction) {
        try {
            alertAction.run();
        } catch (Exception e) {
            log.warn("[outbox] Discord 알림 전송 중 예외 - error: {}", e.getMessage());
        }
    }

    private String failureMessage(Long outboxId) {
        return "❌ 알림 발행 실패 시작 - outboxId: " + outboxId + " (재시도 중)";
    }

    private String recoveredMessage(Long outboxId) {
        return "✅ 알림 발행 복구됨 - outboxId: " + outboxId;
    }
}
