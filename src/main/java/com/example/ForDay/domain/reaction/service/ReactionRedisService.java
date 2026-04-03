package com.example.ForDay.domain.reaction.service;

import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.global.common.constants.CacheConstants;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ReactionRedisService {
    private final RedisTemplate<String, String> redisTemplate;
    public void createReactionWithRedis(String userId, Long recordId, RecordReactionType type) {
        validateDuplicateReaction(recordId, userId, type);

        String value = String.format(CacheConstants.REACTION_QUEUE_FORMAT, userId, recordId, type.name());
        redisTemplate.opsForList().rightPush(CacheConstants.REACTION_QUEUE, value);
    }

    private void validateDuplicateReaction(Long recordId, String userId, RecordReactionType type) {
        String lockKey = String.format(CacheConstants.REACTION_LOCK_KEY, recordId, userId, type.name());

        Boolean isFirstRequest = redisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofSeconds(5));

        if (Boolean.FALSE.equals(isFirstRequest)) {
            throw new CustomException(ErrorCode.DUPLICATE_REACTION);
        }
    }

}
