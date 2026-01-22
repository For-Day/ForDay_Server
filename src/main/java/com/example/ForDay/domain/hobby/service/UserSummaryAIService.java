package com.example.ForDay.domain.hobby.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserSummaryAIService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final int TTL_DAYS = 7; // 7일 유지

    /**
     * 저장된 요약문이 있는지 확인
     */
    public boolean hasSummary(String userSocialId, Long hobbyId) {
        String key = generateKey(userSocialId, hobbyId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * 요약문 저장 (7일 TTL)
     * @param userSummaryText AI가 생성한 요약 문구
     */
    public void saveSummary(String userSocialId, Long hobbyId, String userSummaryText) {
        String key = generateKey(userSocialId, hobbyId);
        redisTemplate.opsForValue().set(key, userSummaryText, TTL_DAYS, TimeUnit.DAYS);
    }

    /**
     * 저장된 요약문 가져오기
     * @return 요약문이 없으면 null 반환
     */
    public String getSummary(String userSocialId, Long hobbyId) {
        String key = generateKey(userSocialId, hobbyId);
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 키 생성 전략 (날짜 제외 -> 7일간 동일 키 유지)
     */
    private String generateKey(String userSocialId, Long hobbyId) {
        return "ai:user:summary:text:" + userSocialId + ":" + hobbyId;
    }
}