package com.example.ForDay.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, String> redisTemplate;

    // 키 저장 (값은 상관없으므로 "true" 저장, TTL은 하루 24시간)
    public void setDataExpire(String key, String value, long duration) {
        Duration expireDuration = Duration.ofSeconds(duration);
        redisTemplate.opsForValue().set(key, value, expireDuration);
    }

    // 키 존재 여부 확인
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // "record:2024-05-20:user1:hobby5" 형식의 키 생성
    public String createRecordKey(String userId, Long hobbyId) {
        String today = LocalDate.now().toString();
        return "record:" + today + ":" + userId + ":" + hobbyId;
    }
}
