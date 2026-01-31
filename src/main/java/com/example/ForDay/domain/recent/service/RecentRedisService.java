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
    private static final int MAX_SIZE = 20; // 최대 20개 유지
    private static final int VIEW_SIZE = 5;

    public void createRecentKeyword(String userId, String keyword) {
        String key = KEY_PREFIX + userId; // key 생성
        double now = (double) System.currentTimeMillis();

        // 1. 중복 제거 및 최신화 (ZSet의 add는 기존 멤버가 있으면 Score만 갱신함)
        redisTemplate.opsForZSet().add(key, keyword, now);

        // 2. 20개 초과분 삭제 (0번부터 -21번까지 삭제하여 상위 20개 유지)
        Long size = redisTemplate.opsForZSet().size(key);
        if (size != null && size > MAX_SIZE) {
            redisTemplate.opsForZSet().removeRange(key, 0, size - MAX_SIZE - 1);
        }

        redisTemplate.expire(key, 30, TimeUnit.DAYS);
    }


     // 전체 목록 최신순 조회
     @Transactional(readOnly = true)
     public GetRecentKeywordResDto getRecentKeywords(String userId) {
         String key = KEY_PREFIX + userId;

         Set<ZSetOperations.TypedTuple<String>> typedTuples =
                 redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, VIEW_SIZE - 1);

         if (typedTuples == null) return new GetRecentKeywordResDto(List.of());

         List<GetRecentKeywordResDto.RecentDto> recentList = typedTuples.stream()
                 .map(tuple -> {
                     String keyword = tuple.getValue();

                     long timestamp = tuple.getScore().longValue();
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
    public void deleteAllRecentKeywords(String userId) {
        String key = KEY_PREFIX + userId;

        Set<ZSetOperations.TypedTuple<String>> allItems =
                redisTemplate.opsForZSet().reverseRangeWithScores(key, 0, -1);

        if (allItems == null || allItems.isEmpty()) {
            return ;
        }

        redisTemplate.delete(key);
    }
}