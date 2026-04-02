package com.example.ForDay.domain.record.service;

import com.example.ForDay.global.common.constants.CacheConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordRedisService {
    private final RedisTemplate<String, String> redisTemplate;

    public void evictRecordCache(Long recordId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    performEvict(recordId);
                }
            });
        } else {
            performEvict(recordId);
        }
    }

    private void performEvict(Long recordId) {
        // 단일 레코드이므로 패턴을 정확하게 생성
        String pattern = String.format(CacheConstants.RECORD_KEY_PATTERN, recordId);
        deleteKeysByPattern(pattern);
    }

    private void deleteKeysByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            Long count = redisTemplate.delete(keys);
            log.info("패턴 [{}]으로 기록 캐시 {}건 삭제 완료", pattern, count);
        }
    }
}
