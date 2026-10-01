package com.example.ForDay.domain.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 같은 알림이 두 번 발송되는 것을 막는다.
 *
 * <p>아웃박스와 메시지 브로커는 둘 다 at-least-once다. 릴레이가 발행에는 성공했는데 상태
 * 갱신에 실패하면 행이 PENDING으로 남아 다음 주기에 다시 발행되고, 브로커도 ack를 받지
 * 못하면 재전달한다. 즉 <b>중복 배달은 정상 동작이며, 없앨 수 있는 대상이 아니다.</b>
 * 딱 한 번 보내려면 받는 쪽에서 "이미 처리한 알림인가"를 판정해야 한다.
 *
 * <p>실측: 30% 확률로 "발송 성공 후 상태 갱신 실패"를 주입했을 때, 이 장치가 없으면 중복이
 * 발생한다(3단계에서 28건 관측).
 *
 * <p>발송 <b>전에</b> 선점하고 실패하면 되돌린다. 발송 후에 기록하면 기록 직전에 죽었을 때
 * 다시 보내게 되고, 선점만 하고 되돌리지 않으면 발송이 실패한 알림이 영영 재시도되지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationDeduplicator {

    private static final String KEY_PREFIX = "notification:dispatched:";
    // 브로커 장애로 재전달이 한참 뒤에 올 수 있어 넉넉히 잡는다. 알림 1건당 키 1개라
    // 하루치를 들고 있어도 메모리 부담이 크지 않다.
    private static final Duration TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;

    /**
     * 이 알림의 발송 권한을 선점한다.
     *
     * @return 처음 보는 알림이면 {@code true}(발송할 것), 이미 처리된 알림이면 {@code false}
     */
    public boolean claim(Long notificationId) {
        if (notificationId == null) {
            // 식별자가 없으면 동일성을 판정할 수 없다. 안 보내는 것보다 중복 위험을 지는 편이 낫다.
            return true;
        }
        Boolean firstTime = redisTemplate.opsForValue()
                .setIfAbsent(KEY_PREFIX + notificationId, "1", TTL);
        return Boolean.TRUE.equals(firstTime);
    }

    /** 발송에 실패했을 때 선점을 되돌린다. 그래야 다음 재전달이 정상적으로 처리된다. */
    public void release(Long notificationId) {
        if (notificationId != null) {
            redisTemplate.delete(KEY_PREFIX + notificationId);
        }
    }
}
