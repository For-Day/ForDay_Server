package com.example.ForDay.global.measure;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 알림 파이프라인 측정 카운터. {@code measure} 프로파일 전용.
 *
 * <p>카운터를 JVM 힙이 아니라 Redis에 두는 이유 — 유실 실험에서 프로세스를 강제 종료하기
 * 때문이다. 인메모리 카운터는 종료와 함께 사라져서 "몇 건이 실제로 나갔고 몇 건이 사라졌는가"를
 * 셀 수 없다.
 *
 * <p>유실은 <b>클라이언트가 받은 응답 수가 아니라 서버가 발송하기로 확정한 건수</b>를 기준으로
 * 센다. 1차 측정에서 이 구분을 하지 않아, 수신자가 푸시를 꺼둔 정상 케이스 132건이 유실로
 * 잘못 잡혔다. {@code accepted}는 "토큰이 있어 실제로 보낼 것"이 확정된 순간에만 오른다.
 */
@Component
@Profile("measure")
@RequiredArgsConstructor
public class MeasurementRecorder {

    public static final String KEY_PREFIX = "measure:";
    public static final String ACCEPTED_KEY = KEY_PREFIX + "accepted";
    public static final String SENT_KEY = KEY_PREFIX + "sent";
    public static final String DUPLICATE_KEY = KEY_PREFIX + "duplicates";
    public static final String DUPLICATE_IDS_KEY = KEY_PREFIX + "duplicate-ids";
    /**
     * reset 시점의 outbox 행 수. 아웃박스 경로(3·4단계)는 "보내기로 확정한 건수"가 곧 저장된
     * outbox 행 수인데, 행은 실행이 끝나도 테이블에 남는다. 테이블을 비우는 대신 기준선을
     * 저장해 증분으로 센다 - 이전 단계의 데이터를 지우지 않아도 되고, 나중에 다시 들여다볼 수 있다.
     */
    public static final String OUTBOX_BASELINE_KEY = KEY_PREFIX + "outbox-baseline";
    public static final String PER_NOTIFICATION_PREFIX = KEY_PREFIX + "id:";

    private final StringRedisTemplate redisTemplate;

    /** 발송이 확정된 순간(수신 토큰이 실제로 존재할 때) 호출한다. 유실 계산의 분모가 된다. */
    public void recordAccepted() {
        redisTemplate.opsForValue().increment(ACCEPTED_KEY);
    }

    /**
     * 실제 발송이 일어난 순간 호출한다. 같은 알림이 두 번째로 나가면 중복으로도 함께 센다 —
     * 나중에 키를 훑는 대신 INCR 반환값으로 그 자리에서 판정한다.
     */
    public void recordSent(Long notificationId) {
        redisTemplate.opsForValue().increment(SENT_KEY);

        if (notificationId == null) {
            return;
        }
        Long sendCount = redisTemplate.opsForValue().increment(PER_NOTIFICATION_PREFIX + notificationId);
        if (sendCount != null && sendCount > 1) {
            redisTemplate.opsForValue().increment(DUPLICATE_KEY);
            redisTemplate.opsForSet().add(DUPLICATE_IDS_KEY, String.valueOf(notificationId));
        }
    }

    public long read(String key) {
        String value = redisTemplate.opsForValue().get(key);
        return value == null ? 0L : Long.parseLong(value);
    }

    public Set<String> duplicateIds() {
        Set<String> ids = redisTemplate.opsForSet().members(DUPLICATE_IDS_KEY);
        return ids == null ? Set.of() : ids;
    }

    /**
     * 적어도 한 번은 발송된 알림의 개수.
     *
     * <p>총 발송 건수만으로는 유실과 중복을 구분할 수 없다. 중복이 있으면 총 발송 수가
     * 부풀고, 유실이 있으면 줄어드는데 둘이 상쇄되면 정상처럼 보인다. "몇 종류의 알림이
     * 나갔는가"를 따로 세야 유실 0을 주장할 수 있다.
     */
    public long distinctSentCount() {
        Set<String> keys = redisTemplate.keys(PER_NOTIFICATION_PREFIX + "*");
        return keys == null ? 0 : keys.size();
    }

    /** 단계를 넘어갈 때마다 호출한다. 이전 단계 카운터가 남아 있으면 유실·중복이 섞인다. */
    public long reset(long currentOutboxRows) {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        long deleted = 0;
        if (keys != null && !keys.isEmpty()) {
            Long count = redisTemplate.delete(keys);
            deleted = count == null ? 0 : count;
        }
        redisTemplate.opsForValue().set(OUTBOX_BASELINE_KEY, String.valueOf(currentOutboxRows));
        return deleted;
    }
}
