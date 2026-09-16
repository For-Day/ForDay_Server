package com.example.ForDay.domain.reaction.service;

import com.example.ForDay.global.common.constants.CacheConstants;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis {@code reaction_queue} 리스트의 현재 길이를 {@code reaction_queue_size} 게이지로 노출한다.
 *
 * <p>{@code ReactionScheduler}가 1초마다 최대 1000건씩 이 큐를 비운다. 유입률이 그 상한을
 * 넘으면 길이가 계속 늘어나며 포화 상태를 드러낸다(#376 스파이크 테스트의 핵심 관측 대상).
 * 배경: {@code docs/perf/metrics.md}
 */
@Component
@RequiredArgsConstructor
public class ReactionQueueMetrics implements MeterBinder {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public void bindTo(MeterRegistry registry) {
        Gauge.builder("reaction_queue_size", redisTemplate, this::currentQueueSize)
                .description("Redis reaction_queue 리스트의 현재 길이 - ReactionScheduler가 1초마다 최대 1000건씩 비운다")
                .register(registry);
    }

    private double currentQueueSize(RedisTemplate<String, String> redisTemplate) {
        Long size = redisTemplate.opsForList().size(CacheConstants.REACTION_QUEUE);
        return size == null ? 0 : size;
    }
}
