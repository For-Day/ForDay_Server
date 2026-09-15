package com.example.ForDay.domain.reaction.service;

import com.example.ForDay.domain.reaction.entity.ActivityRecordReaction;
import com.example.ForDay.domain.reaction.entity.ActivityRecordReactionCount;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionCountRepository;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.common.constants.CacheConstants;
import jakarta.annotation.PostConstruct;
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

    // pop과 DB 저장 사이에 WAS가 죽어도 유실되지 않도록, pop한 항목을 임시로 옮겨두는 처리중 큐.
    // DB 반영이 끝난 뒤에만 여기서 제거하고, 남아있는 항목은 기동 시 recoverPendingReactions()로 복구한다.
    private static final String REACTION_PROCESSING_QUEUE = CacheConstants.REACTION_QUEUE + ":processing";
    private static final int BATCH_SIZE = 1000;

    private final RedisTemplate<String, String> redisTemplate;
    private final ActivityRecordReactionRepository recordReactionRepository;
    private final ActivityRecordReactionCountRepository recordReactionCountRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final UserRepository userRepository;

    /**
     * 이전 인스턴스가 pop 이후 ~ DB 저장 완료 전 구간에서 죽어 processing 큐에 남긴 항목을
     * 기동 시 원래 큐로 되돌려 재처리되도록 복구한다(Reliable Queue 패턴).
     *
     * LMOVE(Redis 6.2+) 대신 구버전 Redis에서도 쓸 수 있는 RPOPLPUSH를 사용한다.
     */
    @PostConstruct
    public void recoverPendingReactions() {
        int recovered = 0;
        while (redisTemplate.opsForList()
                .rightPopAndLeftPush(REACTION_PROCESSING_QUEUE, CacheConstants.REACTION_QUEUE) != null) {
            recovered++;
        }
        if (recovered > 0) {
            log.warn("[reaction] 기동 시 processing 큐에 남아있던 {}건을 reaction_queue로 복구했습니다.", recovered);
        }
    }

    @Transactional
    @Scheduled(fixedDelay = 1000)
    public void saveReactionsToDb() {
        List<String> rawValues = new ArrayList<>();

        // Redis Queue에서 최대 1000개를 processing 큐로 옮기며 꺼낸다.
        // RPOPLPUSH(RPOP + LPUSH를 원자적으로 묶은 커맨드, Redis 1.2+)는 leftPop과 달리
        // 꺼낸 항목을 processing 큐에 그대로 남겨두므로, 이 시점 이후 DB 저장 전에 죽더라도
        // 항목이 사라지지 않는다. push는 rightPush(ReactionRedisLockService)이므로
        // RPOPLPUSH는 가장 최근에 들어온 항목부터 옮긴다(처리 순서는 LIFO가 되지만,
        // 반응 카운트는 항목별로 독립적이라 처리 순서가 결과에 영향을 주지 않는다).
        while (rawValues.size() < BATCH_SIZE) {
            String value = redisTemplate.opsForList()
                    .rightPopAndLeftPush(CacheConstants.REACTION_QUEUE, REACTION_PROCESSING_QUEUE);
            if (value == null) break;
            rawValues.add(value);
        }

        if (rawValues.isEmpty()) return;

        List<ActivityRecordReaction> reactions = rawValues.stream()
                .map(value -> {
                    // ReactionRedisLockService가 REACTION_QUEUE_FORMAT("%s:%d:%s")으로
                    // userId:recordId:type 순서로 push하므로 그 순서에 맞춰 파싱한다.
                    String[] split = value.split(":");
                    String userId = split[0];
                    Long recordId = Long.parseLong(split[1]);
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
            Long recordId = Long.parseLong(split[1]);
            RecordReactionType type = RecordReactionType.valueOf(split[2]);

            int result = recordReactionCountRepository.increaseCount(recordId, type.toString());
            if (result == 0) {
                recordReactionCountRepository.save(
                        ActivityRecordReactionCount.init(recordId, type)
                );
            }
        });

        // DB 반영이 전부 끝난 뒤에만 processing 큐에서 제거한다.
        // 여기 도달하기 전에 예외가 발생하면 트랜잭션은 롤백되고 항목은 processing 큐에 남아
        // 다음 기동 시 recoverPendingReactions()로 복구된다.
        // processing 큐의 head는 항상 이번 배치에서 옮긴 항목이므로(RPOPLPUSH가 매번 head로 push),
        // 정확히 처리한 개수만큼만 head에서 제거해야 과거 크래시로 남아있던 잔여 항목을 건지지 않는다.
        // count 인자를 받는 LPOP(Redis 6.2+)도 구버전 Redis에서는 쓸 수 없어 단건 leftPop을 반복한다.
        for (int i = 0; i < rawValues.size(); i++) {
            redisTemplate.opsForList().leftPop(REACTION_PROCESSING_QUEUE);
        }

        log.info("리액션 DB 저장 완료: {}건", reactions.size());
    }
}
