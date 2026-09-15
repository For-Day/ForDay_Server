package com.example.ForDay.domain.reaction.service;

import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.reaction.dto.ReactionKeyDto;
import com.example.ForDay.domain.reaction.entity.ActivityRecordReaction;
import com.example.ForDay.domain.reaction.entity.ActivityRecordReactionCount;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
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

    @Mock
    private ReactionIndividualSaveService reactionIndividualSaveService;

    private ReactionScheduler reactionScheduler;

    @BeforeEach
    void setUp() {
        reactionScheduler = new ReactionScheduler(
                redisTemplate, recordReactionRepository, recordReactionCountRepository,
                activityRecordRepository, userRepository, reactionIndividualSaveService
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
            // 사전 중복 확인 결과 기존에 저장된 조합이 없는 상황
            given(recordReactionRepository.findExistingKeysByRecordIds(anyCollection()))
                    .willReturn(Collections.emptyList());

            ActivityRecord activityRecord = ActivityRecord.builder().build();
            User user = User.builder().build();
            given(activityRecordRepository.getReferenceById(42L)).willReturn(activityRecord);
            given(userRepository.getReferenceById("user-123")).willReturn(user);
            given(recordReactionCountRepository.increaseCountBy(42L, RecordReactionType.GREAT.toString(), 1L))
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

        @Test
        @DisplayName("사전 중복 확인에 걸린 항목은 saveAll 대상에서 제외되고 카운트도 증가하지 않는다")
        void saveReactionsToDb_filtersOutExistingReactions_beforeBulkSave() {
            // GIVEN: 배치에 2건이 있는데, 그중 (recordId=1, user-1, GREAT)은 이미 DB에 저장되어 있는 상황
            given(listOperations.rightPopAndLeftPush(MAIN_QUEUE, PROCESSING_QUEUE))
                    .willReturn("user-1:1:GREAT")
                    .willReturn("user-2:2:AWESOME")
                    .willReturn(null);
            given(recordReactionRepository.findExistingKeysByRecordIds(anyCollection()))
                    .willReturn(List.of(new ReactionKeyDto(1L, "user-1", RecordReactionType.GREAT)));

            given(activityRecordRepository.getReferenceById(2L)).willReturn(ActivityRecord.builder().build());
            given(userRepository.getReferenceById("user-2")).willReturn(User.builder().build());
            given(recordReactionCountRepository.increaseCountBy(2L, RecordReactionType.AWESOME.toString(), 1L))
                    .willReturn(1);

            // WHEN
            reactionScheduler.saveReactionsToDb();

            // THEN: 사전 중복으로 걸러진 (recordId=1) 건은 saveAll 대상에서 빠지고, 나머지 1건만 저장 시도된다
            ArgumentCaptor<List<ActivityRecordReaction>> captor = ArgumentCaptor.forClass(List.class);
            verify(recordReactionRepository).saveAll(captor.capture());
            assertThat(captor.getValue()).hasSize(1);
            assertThat(captor.getValue().get(0).getReactionType()).isEqualTo(RecordReactionType.AWESOME);

            // THEN: 걸러진 건은 카운트도 증가하지 않는다
            verify(recordReactionCountRepository, never())
                    .increaseCountBy(eq(1L), eq(RecordReactionType.GREAT.toString()), anyLong());
            verify(recordReactionCountRepository, times(1))
                    .increaseCountBy(2L, RecordReactionType.AWESOME.toString(), 1L);

            // THEN: 사전에 걸러졌으므로 건별 재시도(레이스 컨디션 대응 경로)는 호출되지 않는다
            verify(reactionIndividualSaveService, never()).saveIfNotDuplicate(any(ActivityRecordReaction.class));

            // THEN: 사전 중복 확인으로 걸러졌든 저장에 성공했든, 이번 배치 2건은 모두 processing 큐에서 정리된다
            verify(listOperations, times(2)).leftPop(PROCESSING_QUEUE);
        }

        @Test
        @DisplayName("사전 확인을 통과했는데도 벌크 저장이 실패하면(레이스 컨디션) 건별로 재시도하고, 실제로 저장에 성공한 건만 카운트를 증가시킨다")
        void saveReactionsToDb_fallsBackToIndividualSave_whenBulkSaveFailsDespitePreCheck() {
            // GIVEN: 사전 확인에서는 중복이 없다고 나왔지만(레이스 컨디션 상황을 흉내),
            // 그 사이 v1 동기 반응 API 등으로 실제로는 하나가 먼저 저장되어 saveAll이 실패하는 상황
            given(listOperations.rightPopAndLeftPush(MAIN_QUEUE, PROCESSING_QUEUE))
                    .willReturn("u1:1:GREAT")
                    .willReturn("u2:2:AWESOME")
                    .willReturn(null);
            given(recordReactionRepository.findExistingKeysByRecordIds(anyCollection()))
                    .willReturn(Collections.emptyList());
            willThrow(new DataIntegrityViolationException("duplicate"))
                    .given(recordReactionRepository).saveAll(anyList());

            // 건별 재시도에서는 첫 건(u1:1:GREAT)만 실제로 저장에 성공하고,
            // 두 번째 건(u2:2:AWESOME)은 그 사이 먼저 저장된 중복이라 스킵된다.
            given(reactionIndividualSaveService.saveIfNotDuplicate(any(ActivityRecordReaction.class)))
                    .willReturn(true, false);

            // WHEN
            reactionScheduler.saveReactionsToDb();

            // THEN: 저장에 실제로 성공한 건(recordId=1, GREAT)만 카운트가 증가한다
            verify(recordReactionCountRepository, times(1))
                    .increaseCountBy(1L, RecordReactionType.GREAT.toString(), 1L);
            // THEN: 중복으로 스킵된 건(recordId=2, AWESOME)은 카운트가 증가하지 않는다
            verify(recordReactionCountRepository, never())
                    .increaseCountBy(eq(2L), eq(RecordReactionType.AWESOME.toString()), anyLong());

            // THEN: 저장 성공/중복 스킵 여부와 무관하게, 이번 배치에서 옮긴 2건은 모두 processing 큐에서 정리된다
            verify(listOperations, times(2)).leftPop(PROCESSING_QUEUE);
        }

        @Test
        @DisplayName("같은 (recordId, type) 조합이 배치에 여러 건 있어도 카운트 UPDATE는 조합당 1번만 나간다")
        void saveReactionsToDb_groupsSameRecordAndTypeIntoSingleCountUpdate() {
            // GIVEN: 서로 다른 유저 3명이 같은 record(1)에 같은 타입(GREAT)으로 반응한 상황
            given(listOperations.rightPopAndLeftPush(MAIN_QUEUE, PROCESSING_QUEUE))
                    .willReturn("user-a:1:GREAT")
                    .willReturn("user-b:1:GREAT")
                    .willReturn("user-c:1:GREAT")
                    .willReturn(null);
            given(recordReactionRepository.findExistingKeysByRecordIds(anyCollection()))
                    .willReturn(Collections.emptyList());
            given(activityRecordRepository.getReferenceById(1L)).willReturn(ActivityRecord.builder().build());
            given(userRepository.getReferenceById(anyString())).willReturn(User.builder().build());
            given(recordReactionCountRepository.increaseCountBy(1L, RecordReactionType.GREAT.toString(), 3L))
                    .willReturn(1);

            // WHEN
            reactionScheduler.saveReactionsToDb();

            // THEN: 3건이 (recordId=1, GREAT) 하나의 조합으로 묶여 증가량 3으로 UPDATE가 "딱 1번"만 나간다
            // (건당 UPDATE였다면 increaseCount류 메서드가 3번 호출됐어야 한다)
            verify(recordReactionCountRepository, times(1))
                    .increaseCountBy(1L, RecordReactionType.GREAT.toString(), 3L);
        }

        @Test
        @DisplayName("카운트 row가 아직 없는 조합은 건별 1이 아니라 그룹 증가량으로 바로 초기화된다")
        void saveReactionsToDb_initializesNewCountRowWithGroupedDelta() {
            // GIVEN: 서로 다른 유저 2명이 같은 record(1)에 같은 타입(AMAZING)으로 반응했는데,
            // 아직 해당 record의 카운트 row 자체가 없는 상황(UPDATE 대상 row가 없어 결과가 0건)
            given(listOperations.rightPopAndLeftPush(MAIN_QUEUE, PROCESSING_QUEUE))
                    .willReturn("user-a:1:AMAZING")
                    .willReturn("user-b:1:AMAZING")
                    .willReturn(null);
            given(recordReactionRepository.findExistingKeysByRecordIds(anyCollection()))
                    .willReturn(Collections.emptyList());
            given(activityRecordRepository.getReferenceById(1L)).willReturn(ActivityRecord.builder().build());
            given(userRepository.getReferenceById(anyString())).willReturn(User.builder().build());
            given(recordReactionCountRepository.increaseCountBy(1L, RecordReactionType.AMAZING.toString(), 2L))
                    .willReturn(0);

            // WHEN
            reactionScheduler.saveReactionsToDb();

            // THEN: init()으로 무조건 1을 세팅하는 게 아니라, 그룹 증가량(2)을 그대로 초기값으로 저장해야 한다
            ArgumentCaptor<ActivityRecordReactionCount> captor = ArgumentCaptor.forClass(ActivityRecordReactionCount.class);
            verify(recordReactionCountRepository).save(captor.capture());
            assertThat(captor.getValue().getTotalCount()).isEqualTo(2L);
            assertThat(captor.getValue().getAmazingCount()).isEqualTo(2L);
        }
    }
}
