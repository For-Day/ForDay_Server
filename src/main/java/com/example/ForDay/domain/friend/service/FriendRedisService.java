package com.example.ForDay.domain.friend.service;

import com.example.ForDay.global.common.constants.CacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendRedisService {
    private final RedisTemplate<String, String> redisTemplate;
    public void evictFriendCache(String currentUserId, String targetUserId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    performEvict(currentUserId, targetUserId);
                }
            });
        } else {
            performEvict(currentUserId, targetUserId);
        }
    }

    private void performEvict(String currentUserId, String targetUserId) {
        String pattern = String.format(CacheConstants.FRIEND_RELATIONS_KEY_PATTERN, currentUserId, targetUserId);
        deleteKeysByPattern(pattern);
    }

    private void deleteKeysByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            Long count = redisTemplate.delete(keys);
            log.info("패턴 [{}]으로 캐시 {}건 삭제 완료", pattern, count);
        }
    }
}
