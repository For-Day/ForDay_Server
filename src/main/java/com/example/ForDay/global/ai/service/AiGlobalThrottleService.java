package com.example.ForDay.global.ai.service;

import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiGlobalThrottleService {

    private static final long MIN_INTERVAL_MS = 2000;
    private static final String KEY = "openai:last-call";

    private final RedisTemplate<String, Long> redisTemplate;

    public void check() {
        Long last = redisTemplate.opsForValue().get(KEY);
        long now = System.currentTimeMillis();

        if (last != null && now - last < MIN_INTERVAL_MS) {
            throw new CustomException(ErrorCode.AI_RATE_LIMIT_EXCEEDED);
        }

        redisTemplate.opsForValue().set(KEY, now);
    }
}
