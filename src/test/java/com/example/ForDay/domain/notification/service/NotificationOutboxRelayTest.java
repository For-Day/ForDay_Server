package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.notification.entity.NotificationOutbox;
import com.example.ForDay.domain.notification.repository.NotificationOutboxRepository;
import com.example.ForDay.domain.notification.type.OutboxStatus;
import com.example.ForDay.global.port.DiscordAlertPort;
import com.example.ForDay.global.rabbitmq.dto.NotificationEventDto;
import com.example.ForDay.support.IntegrationTestSupport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link NotificationOutboxRelay}(+ {@link NotificationOutboxItemPublisher})가 PENDING 행을
 * 발행하고, 실패·복구 시 Discord edge-trigger가 정확히 한 번만 울리는지 검증한다.
 *
 * <p>{@code notificationId}는 이 테스트에서 실제 {@code Notification} 행을 참조하지 않는다
 * (외래키 관계가 아니라 추적용 평범한 Long이므로) - 그래서 User·Record 등 전체 픽스처 없이
 * outbox 행만 직접 만들어 검증한다.
 */
class NotificationOutboxRelayTest extends IntegrationTestSupport {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private DiscordAlertPort discordAlertPort;

    @Autowired
    private NotificationOutboxRelay relay;

    @Autowired
    private NotificationOutboxRepository outboxRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @AfterEach
    void tearDown() {
        outboxRepository.deleteAll();
    }

    @Test
    @DisplayName("PENDING 행을 발행하면 RabbitMQ로 나가고 PUBLISHED로 바뀐다")
    void pending_행을_발행하면_PUBLISHED가_된다() throws Exception {
        NotificationOutbox outbox = outboxRepository.saveAndFlush(NotificationOutbox.pending(1L, samplePayload()));

        relay.relay();

        NotificationOutbox reloaded = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(reloaded.getPublishedAt()).isNotNull();
        assertThat(reloaded.hadFailed()).isFalse();

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(NotificationEventDto.class));
        verify(discordAlertPort, never()).send(anyString());
    }

    @Test
    @DisplayName("발행이 계속 실패하면 최초 1회만 Discord 알림이 오고 PENDING을 유지한다")
    void 발행_실패시_최초_1회만_알림이_온다() throws Exception {
        NotificationOutbox outbox = outboxRepository.saveAndFlush(NotificationOutbox.pending(2L, samplePayload()));
        willThrow(new AmqpException("연결 실패")).given(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(NotificationEventDto.class));

        relay.relay(); // 1차 실패 - 최초이므로 알림
        relay.relay(); // 2차 실패 - 이미 실패한 적 있으므로 알림 없음

        NotificationOutbox reloaded = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(reloaded.hadFailed()).isTrue();

        verify(discordAlertPort, times(1)).send(contains("실패"));
        verify(discordAlertPort, never()).send(contains("복구"));
    }

    @Test
    @DisplayName("실패했다가 나중에 성공하면 PUBLISHED로 바뀌고 복구 알림이 정확히 1회 온다")
    void 실패_후_복구되면_복구_알림이_온다() throws Exception {
        NotificationOutbox outbox = outboxRepository.saveAndFlush(NotificationOutbox.pending(3L, samplePayload()));

        willThrow(new AmqpException("연결 실패")).given(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(NotificationEventDto.class));
        relay.relay(); // 실패 - PENDING, failedAt 기록, 실패 알림 1회

        willAnswer(invocation -> null).given(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), any(NotificationEventDto.class));
        relay.relay(); // 이번엔 성공 - PUBLISHED, 복구 알림 1회

        NotificationOutbox reloaded = outboxRepository.findById(outbox.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);

        verify(discordAlertPort, times(1)).send(contains("실패"));
        verify(discordAlertPort, times(1)).send(contains("복구"));
    }

    @Test
    @DisplayName("이미 PUBLISHED인 행은 다시 발행되지 않는다 - 블루-그린 동시 릴레이 시 중복 방지")
    void 이미_발행된_행은_다시_처리되지_않는다() throws Exception {
        outboxRepository.saveAndFlush(NotificationOutbox.pending(4L, samplePayload()));

        relay.relay(); // 1차 - 정상 발행
        relay.relay(); // 2차 - 이미 PUBLISHED라 findPendingIds에 안 잡혀야 함

        verify(rabbitTemplate, times(1)).convertAndSend(anyString(), anyString(), any(NotificationEventDto.class));
    }

    private String samplePayload() throws Exception {
        NotificationEventDto dto = NotificationEventDto.builder()
                .receiverId("receiver-id")
                .fcmTokens(List.of("sample-fcm-token"))
                .title("반응 알림")
                .body("누군가 반응을 남겼어요")
                .data(Map.of("recordId", "1"))
                .build();
        return objectMapper.writeValueAsString(dto);
    }
}
