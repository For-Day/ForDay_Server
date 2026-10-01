package com.example.ForDay.domain.reaction.service;

import com.example.ForDay.domain.reaction.entity.ActivityRecordReaction;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionCountRepository;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 2단계(비동기 처리) 측정 전용. {@code measure} 프로파일에서만 빈으로 등록된다.
 *
 * <p>반응 저장 + 카운트 upsert만 응답 스레드 밖(@Async)으로 뺀다 - 중복확인/랭킹 갱신은
 * {@link ReactionAsyncMeasurementService}에서 여전히 동기로 처리해 v1과 조건을 맞춘다.
 * 이 클래스가 큐에 쌓이는 동안 프로세스가 죽으면 작업이 그대로 사라진다 - 그게 이 단계가
 * 보여주려는 것이다(3단계 Redis 큐와의 대조군).
 */
@Component
@Profile("measure")
@RequiredArgsConstructor
public class ReactionAsyncDispatcher {
    private final ActivityRecordReactionRepository recordReactionRepository;
    private final ActivityRecordReactionCountRepository recordReactionCountRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final UserRepository userRepository;

    @Async
    @Transactional
    public void dispatch(Long recordId, RecordReactionType type, String userId) {
        ActivityRecordReaction reaction = ActivityRecordReaction.of(
                activityRecordRepository.getReferenceById(recordId),
                userRepository.getReferenceById(userId),
                type);
        recordReactionRepository.save(reaction);
        recordReactionCountRepository.upsertIncreaseCount(recordId, type.name());
    }
}
