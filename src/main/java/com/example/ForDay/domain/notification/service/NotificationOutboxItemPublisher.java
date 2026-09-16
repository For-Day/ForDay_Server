package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.notification.entity.NotificationOutbox;
import com.example.ForDay.domain.notification.repository.NotificationOutboxRepository;
import com.example.ForDay.global.rabbitmq.config.RabbitMqConfig;
import com.example.ForDay.global.rabbitmq.dto.NotificationEventDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * outbox 행 한 건의 클레임(행 잠금) + 발행 + 상태 갱신을 한 트랜잭션으로 처리한다.
 *
 * <p>{@link NotificationOutboxRelay#relay()}가 이 클래스를 별도 빈으로 호출하는 이유 —
 * 같은 클래스 안에서 {@code this.relayOne(id)}처럼 자기 자신을 호출하면 Spring AOP 프록시를
 * 거치지 않아 {@code @Transactional}이 적용되지 않는다({@code ReactionIndividualSaveService}가
 * 같은 이유로 분리되어 있는 것과 동일한 문제). 그래서 트랜잭션이 필요한 단위를 별도 빈으로 뺐다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOutboxItemPublisher {

    private final NotificationOutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * <b>예외를 다시 던지지 않는다.</b> {@code @Transactional} 롤백에 기대면
     * {@code markFailed()}로 남긴 "최초 실패 시각" 자체가 롤백되어, 재시도마다 매번
     * "처음 실패한 것"처럼 보인다(Discord edge-trigger가 불가능해진다). 그래서 여기서
     * 예외를 직접 흡수하고 상태를 명시적으로 커밋한다.
     */
    @Transactional
    public RelayResult publish(Long id) {
        NotificationOutbox event = outboxRepository.findByIdForUpdate(id).orElse(null);
        if (event == null || event.isPublished()) {
            // 다른 인스턴스(블루-그린 전환 구간)가 락을 풀어주고 나서 보니 이미 처리한 경우.
            return RelayResult.NOOP;
        }

        try {
            NotificationEventDto dto = objectMapper.readValue(event.getPayload(), NotificationEventDto.class);
            rabbitTemplate.convertAndSend(RabbitMqConfig.NOTIFICATION_EXCHANGE, RabbitMqConfig.NOTIFICATION_ROUTING_KEY, dto);

            boolean wasFailing = event.hadFailed();
            event.markPublished();
            return wasFailing ? RelayResult.RECOVERED : RelayResult.NOOP;
        } catch (Exception e) {
            boolean isFirstFailure = !event.hadFailed();
            event.markFailed();
            log.warn("[outbox] 발행 실패 - outboxId: {}, notificationId: {}, error: {}",
                    id, event.getNotificationId(), e.getMessage());
            return isFirstFailure ? RelayResult.FIRST_FAILURE : RelayResult.NOOP;
        }
    }

    public enum RelayResult {
        NOOP, FIRST_FAILURE, RECOVERED
    }
}
