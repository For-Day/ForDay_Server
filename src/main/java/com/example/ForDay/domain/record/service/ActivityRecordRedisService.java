package com.example.ForDay.domain.record.service;

import com.example.ForDay.domain.hobby.dto.response.GetStickerInfoResDto;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ActivityRecordRedisService {

    private final ActivityRecordRepository activityRecordRepository;
    private final RedisTemplate<String, String> redisTemplate;

    @Cacheable(
            value = "stickers",
            key = "#hobbyId + ':' + #userId + ':p' + #page + ':s' + #size",
            unless = "#result == null"
    )
    public List<GetStickerInfoResDto.StickerDto> getCachedStickers(Long hobbyId, Integer page, Integer size, String userId) {
        return activityRecordRepository.getStickerInfo(hobbyId, page, size, userId);
    }

    public void evictStickerCache(Long hobbyId, String userId) {
        // 현재 활성화된 트랜잭션이 있는지 확인
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 트랜잭션 커밋 성공 시 실행될 로직
                    executeEvict(hobbyId, userId);
                }
            });
        } else {
            // 트랜잭션이 없는 상태에서 호출된 경우 즉시 삭제
            executeEvict(hobbyId, userId);
        }
    }

    private void executeEvict(Long hobbyId, String userId) {
        String pattern = "stickers::" + hobbyId + ":" + userId + ":*";
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
