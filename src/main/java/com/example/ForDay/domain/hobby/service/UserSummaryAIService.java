package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.domain.hobby.dto.request.ActivitySummaryRequest;
import com.example.ForDay.domain.hobby.dto.response.ActivitySummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSummaryAIService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final int TTL_DAYS = 7; // 7일 유지

    @Value("${fastapi.url}")
    private String fastApiBaseUrl;
    private final RestTemplate restTemplate;

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

    public String fetchAndSaveUserSummary(String userId, String socialId, Long hobbyId, String hobbyName) {
        try {
            // 1. 요청 DTO 구성
            ActivitySummaryRequest requestDto = ActivitySummaryRequest.builder()
                    .userId(userId)
                    .userHobbyId(hobbyId)
                    .hobbyName(hobbyName)
                    .build();

            String fastapiUrl = fastApiBaseUrl + "/ai/summary";

            // 2. FastAPI 호출 및 DTO 응답 받기
            ActivitySummaryResponse response = restTemplate.postForObject(
                    fastapiUrl,
                    requestDto,
                    ActivitySummaryResponse.class
            );

            // 3. 결과 처리
            if (response != null && response.getSummary() != null) {
                String summary = response.getSummary();

                // Redis에 7일간 저장
                saveSummary(socialId, hobbyId, summary);
                return summary;
            }
        } catch (Exception e) {
            log.error("FastAPI 요약 요청 실패 | socialId: {}, hobbyId: {}, error: {}",
                    socialId, hobbyId, e.getMessage());
        }

        // 예외 발생 시 기본 가이드 문구 반환
        return "";
    }
}