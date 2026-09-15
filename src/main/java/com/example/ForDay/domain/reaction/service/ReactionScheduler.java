package com.example.ForDay.domain.reaction.service;

import com.example.ForDay.domain.reaction.dto.ReactionKeyDto;
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
import java.util.Set;
import java.util.stream.Collectors;

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
    private final ReactionIndividualSaveService reactionIndividualSaveService;

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

        List<ParsedReaction> parsedReactions = rawValues.stream().map(this::parse).toList();

        // 벌크 저장을 시도하기 전에, 이번 배치의 recordId들에 대해 이미 저장된
        // (recordId, userId, type) 조합을 한 번의 쿼리로 조회해 자바 메모리에서 미리 걸러낸다.
        // saveAll은 배치 단위로 실패하므로, 저장을 시도하고 실패를 기다리는 대신
        // 저장 전에 중복을 제거해 대부분의 배치가 예외 없이 한 번에 끝나게 한다.
        Set<Long> recordIds = parsedReactions.stream().map(ParsedReaction::recordId).collect(Collectors.toSet());
        Set<String> existingKeys = recordReactionRepository.findExistingKeysByRecordIds(recordIds).stream()
                .map(ReactionKeyDto::toKey)
                .collect(Collectors.toSet());
        List<ParsedReaction> newReactions = parsedReactions.stream()
                .filter(parsed -> !existingKeys.contains(parsed.toKey()))
                .toList();

        int duplicateCount = parsedReactions.size() - newReactions.size();
        if (duplicateCount > 0) {
            log.info("[reaction] 사전 중복 확인으로 {}건 스킵", duplicateCount);
        }

        // 실제로 DB에 반영된(=중복이 아니라 새로 저장된) 건만 카운트 반영 대상이 된다.
        List<ParsedReaction> savedReactions;
        if (newReactions.isEmpty()) {
            savedReactions = List.of();
        } else {
            List<ActivityRecordReaction> reactions = newReactions.stream().map(this::toEntity).toList();
            try {
                recordReactionRepository.saveAll(reactions);
                recordReactionRepository.flush();
                savedReactions = newReactions;
            } catch (DataIntegrityViolationException e) {
                // 사전 확인 이후에도 실패하는 경우는 레이스 컨디션(예: 큐를 거치지 않는
                // v1 동기 반응 API가 사전 확인과 saveAll 사이에 같은 조합을 먼저 저장한 경우)뿐인
                // 극히 드문 케이스다. 건별로 재시도해 이번에도 중복인 건만 스킵한다.
                log.warn("사전 확인 이후에도 벌크 저장이 실패했습니다(레이스 컨디션 추정). 건별 저장으로 전환합니다.");
                savedReactions = saveIndividually(newReactions);
            }
        }

        // 저장에 실제로 성공한 건수만큼만 카운트를 증가시킨다(중복으로 스킵된 건은 제외).
        savedReactions.forEach(parsed -> {
            int result = recordReactionCountRepository.increaseCount(parsed.recordId(), parsed.type().toString());
            if (result == 0) {
                recordReactionCountRepository.save(
                        ActivityRecordReactionCount.init(parsed.recordId(), parsed.type())
                );
            }
        });

        // DB 반영(저장 성공 또는 중복으로 정상 스킵)이 전부 끝난 뒤에만 processing 큐에서 제거한다.
        // 여기 도달하기 전에 예외가 발생하면 트랜잭션은 롤백되고 항목은 processing 큐에 남아
        // 다음 기동 시 recoverPendingReactions()로 복구된다.
        // processing 큐의 head는 항상 이번 배치에서 옮긴 항목이므로(RPOPLPUSH가 매번 head로 push),
        // 정확히 처리한 개수만큼만 head에서 제거해야 과거 크래시로 남아있던 잔여 항목을 건지지 않는다.
        // count 인자를 받는 LPOP(Redis 6.2+)도 구버전 Redis에서는 쓸 수 없어 단건 leftPop을 반복한다.
        for (int i = 0; i < rawValues.size(); i++) {
            redisTemplate.opsForList().leftPop(REACTION_PROCESSING_QUEUE);
        }

        log.info("리액션 DB 저장 완료: {}건 / 수신 {}건", savedReactions.size(), rawValues.size());
    }

    // 벌크 저장 실패 시 건별로 재시도한다. 한 건의 중복이 다른 건의 저장을 막지 않도록
    // 각 건은 ReactionIndividualSaveService에서 REQUIRES_NEW로 독립된 트랜잭션에서 저장된다.
    private List<ParsedReaction> saveIndividually(List<ParsedReaction> parsedReactions) {
        List<ParsedReaction> saved = new ArrayList<>();
        for (ParsedReaction parsed : parsedReactions) {
            if (reactionIndividualSaveService.saveIfNotDuplicate(toEntity(parsed))) {
                saved.add(parsed);
            }
        }
        return saved;
    }

    private ActivityRecordReaction toEntity(ParsedReaction parsed) {
        return ActivityRecordReaction.builder()
                .activityRecord(activityRecordRepository.getReferenceById(parsed.recordId()))
                .reactedUser(userRepository.getReferenceById(parsed.userId()))
                .reactionType(parsed.type())
                .readWriter(false)
                .build();
    }

    // ReactionRedisLockService가 REACTION_QUEUE_FORMAT("%s:%d:%s")으로
    // userId:recordId:type 순서로 push하므로 그 순서에 맞춰 파싱한다.
    private ParsedReaction parse(String value) {
        String[] split = value.split(":");
        String userId = split[0];
        Long recordId = Long.parseLong(split[1]);
        RecordReactionType type = RecordReactionType.valueOf(split[2]);
        return new ParsedReaction(userId, recordId, type);
    }

    private record ParsedReaction(String userId, Long recordId, RecordReactionType type) {
        // ReactionKeyDto.toKey()와 동일한 형식으로 맞춰야 사전 중복 확인 시 정확히 대조된다.
        String toKey() {
            return recordId + ":" + userId + ":" + type;
        }
    }
}
