package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AiCallCountService {

    private static final int DAILY_LIMIT = 3;

    private final RedisTemplate<String, Integer> redisTemplate;

    public int increaseAndGet(String userSocialId, Long hobbyId) {

        String key = generateKey(userSocialId, hobbyId);

        Integer count = redisTemplate.opsForValue().increment(key).intValue();

        // 최초 생성 시 TTL 설정
        if (count == 1) {
            redisTemplate.expire(key, secondsUntilMidnight(), TimeUnit.SECONDS);
        }

        if (count > DAILY_LIMIT) {
            throw new CustomException(ErrorCode.AI_CALL_LIMIT_EXCEEDED);
        }

        return count;
    }

    public void decrease(String userSocialId, Long hobbyId) {
        String key = generateKey(userSocialId, hobbyId);

        Integer currentCount = redisTemplate.opsForValue().get(key);

        // 값이 있고 0보다 클 때만 감소 (음수 방지)
        if (currentCount != null && currentCount > 0) {
            redisTemplate.opsForValue().decrement(key);
        }
    }

    public int getCurrentCount(String userSocialId, Long hobbyId) {
        String key = generateKey(userSocialId, hobbyId);

        Integer count = redisTemplate.opsForValue().get(key);

        return count != null ? count : 0;
    }

    private String generateKey(String userId, Long hobbyId) {
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return "ai:activity:recommend:" + userId + ":" + hobbyId + ":" + today;
    }

    private long secondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now, midnight).getSeconds();
    }
}

