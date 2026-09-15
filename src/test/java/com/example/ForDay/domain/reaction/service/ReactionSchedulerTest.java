package com.example.ForDay.domain.reaction.service;

import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.reaction.entity.ActivityRecordReaction;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionCountRepository;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReactionSchedulerTest {

    private static final String MAIN_QUEUE = "reaction_queue";
    private static final String PROCESSING_QUEUE = "reaction_queue:processing";

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @Mock
    private ActivityRecordReactionRepository recordReactionRepository;

    @Mock
    private ActivityRecordReactionCountRepository recordReactionCountRepository;

    @Mock
    private ActivityRecordRepository activityRecordRepository;

    @Mock
    private UserRepository userRepository;

    private ReactionScheduler reactionScheduler;

    @BeforeEach
    void setUp() {
        reactionScheduler = new ReactionScheduler(
                redisTemplate, recordReactionRepository, recordReactionCountRepository,
                activityRecordRepository, userRepository
        );
        given(redisTemplate.opsForList()).willReturn(listOperations);
    }

    @Nested
    @DisplayName("기동 시 processing 큐 복구")
    class RecoverPendingReactionsTest {

        @Test
        @DisplayName("processing 큐에 남아있던 항목을 모두 reaction_queue로 되돌린다")
        void recoverPendingReactions_movesLeftoversBackToMainQueue() {
            // GIVEN: 이전 인스턴스가 죽어 processing 큐에 2건이 남아있는 상황
            given(listOperations.rightPopAndLeftPush(PROCESSING_QUEUE, MAIN_QUEUE))
                    .willReturn("user-1:1:GREAT")
                    .willReturn("user-2:2:AWESOME")
                    .willReturn(null);

            // WHEN
            reactionScheduler.recoverPendingReactions();

            // THEN: 큐가 빌 때까지(null이 나올 때까지) RPOPLPUSH를 반복 호출한다 (2건 + 종료 확인 1회)
            verify(listOperations, times(3))
                    .rightPopAndLeftPush(PROCESSING_QUEUE, MAIN_QUEUE);
        }

        @Test
        @DisplayName("processing 큐가 비어있으면 아무 것도 옮기지 않는다")
        void recoverPendingReactions_doesNothingWhenProcessingQueueEmpty() {
            // GIVEN: 정상 종료되어 processing 큐에 남은 게 없는 상황
            given(listOperations.rightPopAndLeftPush(PROCESSING_QUEUE, MAIN_QUEUE))
                    .willReturn(null);

            // WHEN
            reactionScheduler.recoverPendingReactions();

            // THEN: 최초 1회 확인만 하고 끝난다
            verify(listOperations, times(1))
                    .rightPopAndLeftPush(PROCESSING_QUEUE, MAIN_QUEUE);
        }
    }

    @Nested
    @DisplayName("리액션 큐 DB 반영")
    class SaveReactionsToDbTest {

        @Test
        @DisplayName("큐가 비어있으면 저장도, processing 큐 정리도 하지 않는다")
        void saveReactionsToDb_doesNothingWhenQueueEmpty() {
            // GIVEN: 큐에 반영할 반응이 하나도 없는 상황
            given(listOperations.rightPopAndLeftPush(MAIN_QUEUE, PROCESSING_QUEUE))
                    .willReturn(null);

            // WHEN
            reactionScheduler.saveReactionsToDb();

            // THEN: 빈 배치에 대해 불필요한 DB/Redis 호출이 나가지 않는다
            verify(recordReactionRepository, never()).saveAll(anyList());
            verify(listOperations, never()).leftPop(anyString());
        }

        @Test
        @DisplayName("userId:recordId:type 순서로 push된 값을 그 순서에 맞게 파싱해서 저장하고, 저장 성공 후에만 processing 큐를 정리한다")
        void saveReactionsToDb_parsesUserIdBeforeRecordId_andCleansUpOnlyAfterSuccess() {
            // GIVEN: ReactionRedisLockService가 실제로 push하는 포맷("userId:recordId:type")대로 값 1건이 큐에 있는 상황
            given(listOperations.rightPopAndLeftPush(MAIN_QUEUE, PROCESSING_QUEUE))
                    .willReturn("user-123:42:GREAT")
                    .willReturn(null);

            ActivityRecord activityRecord = ActivityRecord.builder().build();
            User user = User.builder().build();
            given(activityRecordRepository.getReferenceById(42L)).willReturn(activityRecord);
            given(userRepository.getReferenceById("user-123")).willReturn(user);
            given(recordReactionCountRepository.increaseCount(42L, RecordReactionType.GREAT.toString()))
                    .willReturn(1);

            // WHEN
            reactionScheduler.saveReactionsToDb();

            // THEN: split[0]=userId, split[1]=recordId 순서로 올바르게 파싱되어 저장됐는지 확인
            ArgumentCaptor<List<ActivityRecordReaction>> captor = ArgumentCaptor.forClass(List.class);
            verify(recordReactionRepository).saveAll(captor.capture());
            List<ActivityRecordReaction> saved = captor.getValue();
            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getActivityRecord()).isEqualTo(activityRecord);
            assertThat(saved.get(0).getReactedUser()).isEqualTo(user);
            assertThat(saved.get(0).getReactionType()).isEqualTo(RecordReactionType.GREAT);

            // THEN: DB 반영(저장+카운트)이 전부 끝난 뒤에만 처리한 건수(1건)만큼 processing 큐를 정리한다
            verify(listOperations, times(1)).leftPop(PROCESSING_QUEUE);
        }
    }
}
