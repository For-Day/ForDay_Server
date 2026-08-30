package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.port.AiInsightPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * AI 활동 요약을 만들지 말지 판단하고, 결과를 캐시한다.
 *
 * <p>원래 {@code global/ai/service/AiUserSummaryService}에 있던 로직이다.
 * "최근 7일 기록이 5건 미만이면 요약하지 않는다"는 도메인 규칙인데 횡단 관심사 패키지에
 * 있었고, 그 때문에 global이 record 리포지토리와 도메인 엔티티를 참조하고 있었다.
 * 판단과 캐시는 여기로 옮기고, global에는 외부 호출만 남겼다(AiInsightPort).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HobbyAiSummaryService {

    public static final String AI_USER_SUMMARY_PREFIX = "ai:user:summary:text";
    public static final String AI_USER_SUMMARY_FORMAT = AI_USER_SUMMARY_PREFIX + ":%s:%s";

    /** 요약을 만들 만큼 기록이 쌓였다고 보는 최소 개수 */
    private static final int MIN_RECORDS_FOR_SUMMARY = 5;
    private static final int RECENT_DAYS = 7;
    private static final int TTL_DAYS = 7;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ActivityRecordRepository activityRecordRepository;
    private final AiInsightPort aiInsightPort;

    /**
     * @return 요약문. 기록이 부족하거나 생성에 실패하면 빈 문자열.
     */
    public String determine(User user, Hobby hobby) {
        LocalDateTime since = LocalDateTime.now().minusDays(RECENT_DAYS);
        long recordCount = activityRecordRepository.countByUserIdAndHobbyIdAndCreatedAtAfterAndDeletedFalse(
                user.getId(), hobby.getId(), since
        );

        if (recordCount < MIN_RECORDS_FOR_SUMMARY) {
            log.info("[GetHomeHobbyInfo] Insufficient records for AI summary (Count: {})", recordCount);
            return "";
        }

        if (hasSummary(user.getSocialId(), hobby.getId())) {
            return getSummary(user.getSocialId(), hobby.getId());
        }

        try {
            String summary = aiInsightPort.requestActivitySummary(
                    user.getId(), hobby.getId(), hobby.getHobbyName());

            if (!summary.isEmpty()) {
                saveSummary(user.getSocialId(), hobby.getId(), summary);
            }
            return summary;
        } catch (Exception e) {
            log.error("Error creating AI summary: {}", e.getMessage());
            return "";
        }
    }

    public boolean hasSummary(String userSocialId, Long hobbyId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(generateKey(userSocialId, hobbyId)));
    }

    public String getSummary(String userSocialId, Long hobbyId) {
        Object value = redisTemplate.opsForValue().get(generateKey(userSocialId, hobbyId));
        return value != null ? value.toString() : null;
    }

    public void saveSummary(String userSocialId, Long hobbyId, String userSummaryText) {
        redisTemplate.opsForValue()
                .set(generateKey(userSocialId, hobbyId), userSummaryText, TTL_DAYS, TimeUnit.DAYS);
    }

    /** 날짜를 넣지 않아 TTL 기간 동안 같은 키를 유지한다. */
    private String generateKey(String userSocialId, Long hobbyId) {
        return String.format(AI_USER_SUMMARY_FORMAT, userSocialId, hobbyId);
    }
}
