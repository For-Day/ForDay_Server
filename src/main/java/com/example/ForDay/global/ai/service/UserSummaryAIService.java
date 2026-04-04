package com.example.ForDay.global.ai.service;

import com.example.ForDay.domain.hobby.dto.request.ActivitySummaryRequest;
import com.example.ForDay.domain.hobby.dto.response.ActivitySummaryResponse;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSummaryAIService {

    public static final String AI_USER_SUMMARY_PREFIX = "ai:user:summary:text";
    public static final String AI_USER_SUMMARY_FORMAT = AI_USER_SUMMARY_PREFIX + ":%s:%s";
    public static final String AI_SUMMARY_PATH = "/ai/summary";

    private final RedisTemplate<String, Object> redisTemplate;
    private static final int TTL_DAYS = 7; // 7일 유지

    @Value("${fastapi.url}")
    private String fastApiBaseUrl;

    private final RestTemplate restTemplate;
    private final ActivityRecordRepository activityRecordRepository;

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
        return String.format(AI_USER_SUMMARY_FORMAT, userSocialId, hobbyId);
    }

    public String fetchAndSaveUserSummary(String userId, String socialId, Long hobbyId, String hobbyName) {
        try {
            ActivitySummaryRequest requestDto = ActivitySummaryRequest.of(userId, hobbyId, hobbyName);

            String fastapiUrl = fastApiBaseUrl + AI_SUMMARY_PATH;

            ActivitySummaryResponse response = restTemplate.postForObject(fastapiUrl, requestDto, ActivitySummaryResponse.class);

            if (response != null && response.getSummary() != null) {
                String summary = response.getSummary();
                saveSummary(socialId, hobbyId, summary);
                return summary;
            }
        } catch (Exception e) {
            log.error("FastAPI 요약 요청 실패 | socialId: {}, hobbyId: {}, error: {}",
                    socialId, hobbyId, e.getMessage());
        }
        return "";
    }

    public String determine(User user, Hobby hobby) {
        // 최근 7일간 기록 개수 확인
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long recordCount = activityRecordRepository.countByUserIdAndHobbyIdAndCreatedAtAfterAndDeletedFalse(
                user.getId(), hobby.getId(), sevenDaysAgo
        );

        if (recordCount < 5) {
            log.info("[GetHomeHobbyInfo] Insufficient records for AI summary (Count: {})", recordCount);
            return "";
        }

        // 캐시된 요약 확인 또는 신규 생성
        if (hasSummary(user.getSocialId(), hobby.getId())) {
            return getSummary(user.getSocialId(), hobby.getId());
        }

        try {
            return fetchAndSaveUserSummary(user.getId(), user.getSocialId(), hobby.getId(), hobby.getHobbyName());
        } catch (Exception e) {
            log.error("Error creating AI summary: {}", e.getMessage());
            return "";
        }
    }
}