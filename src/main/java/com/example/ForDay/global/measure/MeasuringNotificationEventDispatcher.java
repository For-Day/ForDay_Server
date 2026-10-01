package com.example.ForDay.global.measure;

import com.example.ForDay.domain.notification.service.NotificationEventDispatcher;
import com.example.ForDay.domain.notification.service.RabbitNotificationEventDispatcher;
import com.example.ForDay.global.port.PushMessage;
import com.example.ForDay.global.port.PushSenderPort;
import com.example.ForDay.global.rabbitmq.dto.NotificationEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 3·4단계 측정용 발행기. {@code measure} 프로파일에서 운영 발행기를 대체한다.
 *
 * <p>두 가지를 설정으로 바꾼다.
 * <ul>
 *   <li>{@code measure.outbox.target} — {@code direct}면 브로커 없이 릴레이가 직접 FCM을
 *       발송한다(3단계, 순수 아웃박스). {@code rabbit}이면 운영과 같이 RabbitMQ로 보낸다(4단계).</li>
 *   <li>{@code measure.outbox.fail-after-dispatch-rate} — 발송에 성공한 <b>뒤</b> 확률적으로
 *       예외를 던진다. 피드백 문서가 지적한 "외부 발송은 성공했는데 상태 갱신이 실패해 중복이
 *       나간다"를 그대로 재현한다.</li>
 * </ul>
 *
 * <p>장애 주입을 발행기 바깥이 아니라 여기에 둔 이유 — 3단계와 4단계에 <b>똑같은</b> 장애를
 * 넣어야 "중복 N건 → 0건"이 주장으로 성립한다. 두 경로에 따로 넣으면 조건이 어긋난다.
 *
 * <p>{@code @Transactional}을 붙이지 않는다. 붙이면 ArchUnit S5(트랜잭션 안에서 푸시를 직접
 * 발송하지 않는다)에 걸린다.
 */
@Component
@Primary
@Profile("measure")
@RequiredArgsConstructor
public class MeasuringNotificationEventDispatcher implements NotificationEventDispatcher {

    private static final String DIRECT_TARGET = "direct";

    private final RabbitNotificationEventDispatcher rabbitDispatcher;
    private final PushSenderPort pushSenderPort;

    @Value("${measure.outbox.target:rabbit}")
    private String target;

    @Value("${measure.outbox.fail-after-dispatch-rate:0}")
    private double failAfterDispatchRate;

    @Override
    public void dispatch(NotificationEventDto event) {
        if (DIRECT_TARGET.equalsIgnoreCase(target)) {
            sendDirectly(event);
        } else {
            rabbitDispatcher.dispatch(event);
        }

        // 발송 "뒤"에 던지는 것이 핵심이다. 앞에서 던지면 그냥 발송 실패이고, 아웃박스가
        // 정상적으로 재시도해 아무 문제도 드러나지 않는다.
        if (failAfterDispatchRate > 0
                && ThreadLocalRandom.current().nextDouble() < failAfterDispatchRate) {
            throw new IllegalStateException("측정용 장애 주입 - 발송은 성공했으나 상태 갱신에 실패");
        }
    }

    private void sendDirectly(NotificationEventDto event) {
        if (event.getFcmTokens() == null) {
            return;
        }
        for (String token : event.getFcmTokens()) {
            pushSenderPort.send(new PushMessage(
                    event.getNotificationId(), token, event.getTitle(), event.getBody(), event.getData()));
        }
    }
}
