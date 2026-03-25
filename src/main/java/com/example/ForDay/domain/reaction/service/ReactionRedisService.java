package com.example.ForDay.domain.reaction.service;

import com.example.ForDay.domain.record.type.RecordReactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReactionRedisService {
    private final RedisTemplate<String, String> redisTemplate;

    public void createReactionWithRedis(String userId, Long recordId, RecordReactionType type) {
        String value = userId + ":" + recordId + ":" + type;

        redisTemplate.opsForList().rightPush("reaction_queue", value);
    }

}
