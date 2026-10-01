package com.example.ForDay.domain.notification.service;

import com.example.ForDay.global.port.PushMessage;
import com.example.ForDay.global.port.PushSenderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * FCM 발송만 요청 스레드에서 떼어내는 2단계 측정용 컴포넌트.
 *
 * <p>{@link AsyncPushNotificationSender}와 별도 빈으로 나눈 이유 — 같은 클래스 안에서
 * 자기 자신을 호출하면 Spring AOP 프록시를 거치지 않아 {@code @Async}가 적용되지 않는다
 * ({@code NotificationOutboxItemPublisher}를 {@code NotificationOutboxRelay}에서 분리한 것과
 * 같은 이유).
 *
 * <p>{@code @Transactional}을 붙이지 않는다. 붙이면 ArchUnit S5(트랜잭션 안에서 푸시를 직접
 * 발송하지 않는다)에 걸리며, 실제로도 커밋 전에 발송하면 롤백돼도 알림이 나가버린다.
 */
@Slf4j
@Component
@Profile("measure")
@RequiredArgsConstructor
public class AsyncPushDispatcher {

    private final PushSenderPort pushSenderPort;

    @Async
    public void dispatch(Long notificationId, List<String> tokens, String title, String body, Map<String, String> data) {
        for (String token : tokens) {
            try {
                pushSenderPort.send(new PushMessage(notificationId, token, title, body, data));
            } catch (Exception e) {
                log.error("[FCM-Async] 전송 중 에러 - notificationId: {}, error: {}", notificationId, e.getMessage());
            }
        }
    }
}
