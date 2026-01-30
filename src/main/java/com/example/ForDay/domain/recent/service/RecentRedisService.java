package com.example.ForDay.domain.recent.service;

import com.example.ForDay.domain.recent.dto.response.GetRecentKeywordResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RecentRedisService {

    private final RedisTemplate<String, String> redisTemplate;
    private static final String KEY_PREFIX = "recent:"; // recent:userId 형태
    private static final int MAX_SIZE = 5; // 최대 5개 유지

    // 최근 검색어 생성
    public void createRecentKeyword(String userId, String keyword) {
        String key = KEY_PREFIX + userId;
        long now = System.currentTimeMillis();

        // 중복 검색어일 경우 기존 데이터를 지우고 최신 score로 갱신
        redisTemplate.opsForZSet().add(key, keyword, now);

        // 5개 초과분 삭제 (index 0부터 -6까지 삭제하여 최신 5개만 남김)
        redisTemplate.opsForZSet().removeRange(key, 0, -(MAX_SIZE + 1));

        // 유효기간 설정
        redisTemplate.expire(key, 30, TimeUnit.DAYS);
    }


     // 전체 목록 최신순 조회
    @Transactional(readOnly = true)
    public GetRecentKeywordResDto getRecentKeywords(String userId) {
        String key = KEY_PREFIX + userId;

        // 1. Redis에서 검색어와 Score(타임스탬프)를 함께 가져옴
        // ZSet의 Tuple에는 value(keyword)와 score(timestamp)가 들어있음
        Set<ZSetOperations.TypedTuple<String>> typedTuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, MAX_SIZE - 1);

        if (typedTuples == null || typedTuples.isEmpty()) {
            return new GetRecentKeywordResDto(List.of());
        }

        // Tuple을 RecentDto로 변환
        List<GetRecentKeywordResDto.RecentDto> recentList = typedTuples.stream()
                .map(tuple -> {
                    String keyword = tuple.getValue();
                    Long timestamp = tuple.getScore().longValue(); // 저장할 때 넣은 System.currentTimeMillis()

                    // LocalDateTime으로 변환
                    LocalDateTime createdAt = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());

                    return new GetRecentKeywordResDto.RecentDto(timestamp, keyword, createdAt);
                })
                .toList();

        return new GetRecentKeywordResDto(recentList);
    }


    // 개별 검색어 삭제
    public Long deleteRecentKeyword(String userId, Long recentId) {
        String key = KEY_PREFIX + userId;

        Set<String> targets = redisTemplate.opsForZSet().rangeByScore(key, recentId, recentId);

        if (targets != null && !targets.isEmpty()) {
            String keyword = targets.iterator().next();
            redisTemplate.opsForZSet().remove(key, keyword);
            return recentId;
        }

        return null;
    }

    // 전체 검색어 삭제
    public List<Long> deleteAllRecentKeywords(String userId) {
        String key = KEY_PREFIX + userId;

        Set<ZSetOperations.TypedTuple<String>> allItems =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, -1);

        if (allItems == null || allItems.isEmpty()) {
            return List.of();
        }

        List<Long> deletedIds = allItems.stream()
                .map(tuple -> tuple.getScore().longValue())
                .toList();

        redisTemplate.delete(key);

        return deletedIds;
    }
}