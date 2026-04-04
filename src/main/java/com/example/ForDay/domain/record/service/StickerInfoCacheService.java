package com.example.ForDay.domain.record.service;

import com.example.ForDay.domain.hobby.dto.response.GetStickerInfoResDto;
import com.example.ForDay.global.common.constants.CacheConstants;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class StickerInfoCacheService {

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

    public void evictRecordCache(Long hobbyId, String userId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executeEvict(hobbyId, userId);
                }
            });
        } else {
            executeEvict(hobbyId, userId);
        }
    }

    private void executeEvict(Long hobbyId, String userId) {
        String stickerPattern = String.format(CacheConstants.STICKER_KEY_PATTERN, hobbyId, userId);
        String feedPattern = String.format(CacheConstants.USER_FEED_KEY_PATTERN, userId);

        deleteKeysByPattern(stickerPattern);
        deleteKeysByPattern(feedPattern);
    }

    private void deleteKeysByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
