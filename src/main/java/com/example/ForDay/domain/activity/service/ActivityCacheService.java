package com.example.ForDay.domain.activity.service;

import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.dto.response.GetHobbyActivitiesResDto;
import com.example.ForDay.global.common.constants.CacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityCacheService {
    private final ActivityRepository activityRepository;
    private final StringRedisTemplate redisTemplate;

    @Cacheable(
            value = "hobbyActivities",
            key = "#userId + ':' + #hobbyId + ':' + (#size ?: 10)",
            unless = "#result == null"
    )
    public GetHobbyActivitiesResDto getHobbyActivitiesCached(Long hobbyId, String userId, Integer size) {
        return activityRepository.getHobbyActivities(hobbyId, size);
    }

    public void evictCacheAfterCommit(String userId, Long hobbyId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictAllSizeHobbyActivitiesCache(userId, hobbyId);
                }
            });
        } else {
            evictAllSizeHobbyActivitiesCache(userId, hobbyId);
        }
    }

    private void evictAllSizeHobbyActivitiesCache(String userId, Long hobbyId) {
        String pattern = String.format(CacheConstants.HOBBY_ACTIVITIES_KEY_PATTERN, userId, hobbyId);
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("[Redis] 패턴 캐시 제거 완료 - 패턴: {}, 제거된 키 개수: {}", pattern, keys.size());
        } else {
            log.info("[Redis] 제거할 캐시가 없습니다 - 패턴: {}", pattern);
        }
    }
}
