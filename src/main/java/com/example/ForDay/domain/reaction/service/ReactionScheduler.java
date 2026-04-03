package com.example.ForDay.domain.reaction.service;

import com.example.ForDay.domain.reaction.entity.ActivityRecordReaction;
import com.example.ForDay.domain.reaction.entity.ActivityRecordReactionCount;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionCountRepository;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReactionScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final ActivityRecordReactionRepository recordReactionRepository;
    private final ActivityRecordReactionCountRepository recordReactionCountRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final UserRepository userRepository;

    @Transactional
    @Scheduled(fixedDelay = 1000)
    public void saveReactionsToDb() {
        List<String> rawValues = new ArrayList<>();

        // Redis Queue에서 최대 1000개 꺼내기
        while (true) {
            String value = redisTemplate.opsForList().leftPop("reaction_queue");
            if (value == null) break;
            rawValues.add(value);
            if (rawValues.size() >= 1000) break;
        }

        if (rawValues.isEmpty()) return;

        List<ActivityRecordReaction> reactions = rawValues.stream()
                .map(value -> {
                    String[] split = value.split(":");
                    Long recordId = Long.parseLong(split[0]);
                    String userId = split[1];
                    RecordReactionType type = RecordReactionType.valueOf(split[2]);

                    return ActivityRecordReaction.builder()
                            .activityRecord(activityRecordRepository.getReferenceById(recordId))
                            .reactedUser(userRepository.getReferenceById(userId))
                            .reactionType(type)
                            .readWriter(false)
                            .build();
                })
                .toList();

        try {
            recordReactionRepository.saveAll(reactions);
            recordReactionRepository.flush();
        } catch (DataIntegrityViolationException e) {
            log.warn("벌크 저장 중 중복 데이터 발견. 건별 저장으로 전환하거나 무시합니다.");
        }

        rawValues.forEach(value -> {
            String[] split = value.split(":");
            Long recordId = Long.parseLong(split[0]);
            RecordReactionType type = RecordReactionType.valueOf(split[2]);

            int result = recordReactionCountRepository.increaseCount(recordId, type.toString());
            if (result == 0) {
                recordReactionCountRepository.save(
                        ActivityRecordReactionCount.init(recordId, type)
                );
            }
        });

        log.info("리액션 DB 저장 완료: {}건", reactions.size());
    }
}
