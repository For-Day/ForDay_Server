package com.example.ForDay.domain.record.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RedisReactionService {

    private final RedisTemplate<String, Object> redisObjectTemplate;
    private static final String RANKING_KEY = "reaction:ranking:30days";

    /**
     * 반응 추가 시 (+1)
     */
    public void incrementRankingScore(Long recordId) {
        redisObjectTemplate.opsForZSet().incrementScore(RANKING_KEY, recordId.toString(), 1);
    }

    /**
     * 반응 취소 시 (-1)
     */
    public void decrementRankingScore(Long recordId) {
        Double score = redisObjectTemplate.opsForZSet().score(RANKING_KEY, recordId.toString());

        if (score != null && score > 0) {
            redisObjectTemplate.opsForZSet().incrementScore(RANKING_KEY, recordId.toString(), -1);
        }
    }

    public List<Long> getHotRecordIdsByCursor(Double lastScore, Long lastRecordId, int size) {
        // 1. 커서가 없으면 최고 점수부터, 있으면 lastScore 이하부터 조회
        double max = (lastScore != null) ? lastScore : Double.POSITIVE_INFINITY;

        // 2. Redis에서 점수 범위로 ID 추출 (ZREVRANGEBYSCORE)
        // 동일 점수 내에서의 세밀한 커서 처리는 DB 단계에서 수행하는 것이 효율적입니다.
        Set<Object> ids = redisObjectTemplate.opsForZSet()
                .reverseRangeByScore("reaction:ranking:30days", 0, max, 0, size * 2); // 필터링 대비 2배수 추출

        return ids.stream()
                .map(id -> Long.parseLong(id.toString()))
                .collect(Collectors.toList());
    }
}