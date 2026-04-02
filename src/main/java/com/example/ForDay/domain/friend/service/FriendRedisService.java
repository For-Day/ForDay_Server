package com.example.ForDay.domain.friend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendRedisService {
    private final CacheManager cacheManager;
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
        String sortedKey = Stream.of(currentUserId, targetUserId).sorted().collect(Collectors.joining(":"));
        Cache cache = cacheManager.getCache("friendRelations");
        if (cache != null) {
            cache.evict(sortedKey);
            log.info("캐시 무효화 완료 (커밋 후): {}", sortedKey);
        }
    }
}
