package com.example.ForDay.domain.record.service;

import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TodayRecordRedisService {
    public static final String RECORD_KEY_PREFIX = "record";
    public static final String RECORD_KEY_FORMAT = RECORD_KEY_PREFIX + ":%s:%s:%s";

    private final RedisTemplate<String, String> redisTemplate;

    // 키 저장 (값은 상관없으므로 "true" 저장)
    public void setDataExpire(String key, String value) {
        redisTemplate.opsForValue().set(key, value, secondsUntilMidnight());
    }

    // 키 존재 여부 확인
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    // "record:2024-05-20:user1:hobby5" 형식의 키 생성
    public String createRecordKey(String userId, Long hobbyId) {
        String today = LocalDate.now().toString();
        return buildRecordKey(today, userId, hobbyId);
    }

    private long secondsUntilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
        return Duration.between(now, midnight).getSeconds();
    }

    public void deleteTodayRecordKey(String userId, Long hobbyId) {
        String key = createRecordKey(userId, hobbyId);
        redisTemplate.delete(key);
    }

    public void validateNotRecordedToday(String userId, Long hobbyId) {
        if (isRecordedToday(userId, hobbyId)) {
            throw new CustomException(ErrorCode.ALREADY_RECORDED_TODAY);
        }
    }

    public void markAsRecorded(String userId, Long hobbyId) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executeMarkAsRecorded(userId, hobbyId);
                }
            });
        } else {
            executeMarkAsRecorded(userId, hobbyId);
        }
    }

    private void executeMarkAsRecorded(String userId, Long hobbyId) {
        String key = createRecordKey(userId, hobbyId);
        setDataExpire(key, "recorded");
        log.info("[Redis] 기록 완료 상태 저장 - Key: {}", key);
    }

    public boolean isRecordedToday(String userId, Long hobbyId) {
        return hasKey(createRecordKey(userId, hobbyId));
    }

    public static String buildRecordKey(String today, String userId, Long hobbyId) {
        return String.format(RECORD_KEY_FORMAT, today, userId, hobbyId);
    }
}
