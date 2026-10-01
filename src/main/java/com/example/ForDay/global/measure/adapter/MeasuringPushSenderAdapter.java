package com.example.ForDay.global.measure.adapter;

import com.example.ForDay.global.measure.MeasurementRecorder;
import com.example.ForDay.global.port.PushMessage;
import com.example.ForDay.global.port.PushSenderPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 측정용 푸시 어댑터. {@code measure} 프로파일에서 실제 FCM 어댑터를 대체한다.
 *
 * <p>실제 FCM을 초당 수백 건 호출할 수는 없다(레이트 리밋에 걸리고 실제 기기로 푸시가 나간다).
 * 그래서 발송 자체는 하지 않고 <b>네트워크 지연만 흉내</b> 낸 뒤, 발송 사실을 기록한다.
 *
 * <p>"알림 1건 = 발송 1건"이라는 전제는 {@code ReactionMeasurementSeeder}가 유저마다 FCM
 * 토큰을 정확히 하나씩 만들어주기 때문에 성립한다. 실제 서비스처럼 한 유저가 기기를 여러 대
 * 쓰면 정상 발송도 2건이 되므로, 이 전제가 깨지면 중복 집계도 함께 고쳐야 한다.
 *
 * <p>건별 로그를 남기지 않는다. 초당 수백 건 구간에서 동기 파일 I/O가 끼면 측정 대상이
 * 알림 경로가 아니라 로거가 되어버린다.
 */
@Slf4j
@Component
@Primary
@Profile("measure")
@RequiredArgsConstructor
public class MeasuringPushSenderAdapter implements PushSenderPort {

    private final MeasurementRecorder recorder;

    /** FCM 왕복 지연을 흉내 내는 값. 이 값이 스레드 점유 시간을 결정한다. */
    @Value("${measure.push.latency-ms:300}")
    private long latencyMs;

    @Override
    public void send(PushMessage message) {
        sleepQuietly();
        recorder.recordSent(message.notificationId());
    }

    // PushSenderPort 계약상 전송 실패를 예외로 올리지 않는다. 인터럽트도 같은 이유로
    // 삼키되 플래그는 복원해, 종료 신호가 상위에서 처리될 수 있게 한다.
    private void sleepQuietly() {
        if (latencyMs <= 0) {
            return;
        }
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
