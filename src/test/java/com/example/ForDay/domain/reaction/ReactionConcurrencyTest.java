package com.example.ForDay.domain.reaction;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.reaction.entity.ActivityRecordReactionCount;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionCountRepository;
import com.example.ForDay.domain.reaction.service.ReactionService;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.global.oauth.CustomUserDetails;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ReactToRecordConcurrencyTest {

    @Autowired
    private ReactionService reactionService;

    @Autowired
    private ActivityRecordRepository activityRecordRepository;

    @Autowired
    private ActivityRecordReactionCountRepository reactionCountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HobbyRepository hobbyRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private EntityManager entityManager;

    private Long targetRecordId;
    private final List<User> mockUsers = new ArrayList<>();
    private final int threadCount = 30;

    @BeforeEach
    void setUp() {
        // ==================== [GIVEN: 멀티스레드 진입 전 데이터를 완벽하게 DB에 반영] ====================

        // 1. 기록 작성자 유저 저장 후 DB 반영
        User recordWriter = userRepository.saveAndFlush(User.builder()
                .socialId("writer_social_id")
                .nickname("기록작성자")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .deleted(false)
                .build());

        // 2. 취미 저장 후 DB 반영
        Hobby hobby = hobbyRepository.saveAndFlush(Hobby.builder()
                .user(recordWriter)
                .hobbyName("매일 운동하기")
                .hobbyPurpose("체력 증진")
                .hobbyTimeMinutes(45)
                .executionCount(1)
                .status(HobbyStatus.IN_PROGRESS)
                .build());

        // 3. 활동 저장 후 DB 반영
        Activity activity = activityRepository.saveAndFlush(Activity.builder()
                .user(recordWriter)
                .hobby(hobby)
                .content("오늘 오운완 성공했습니다!")
                .build());

        // 4. 대상 활동 기록 저장 후 DB 반영
        ActivityRecord savedRecord = activityRecordRepository.saveAndFlush(ActivityRecord.builder()
                .activity(activity)
                .hobby(hobby)
                .user(recordWriter)
                .sticker("sports_sticker")
                .visibility(RecordVisibility.PUBLIC)
                .build());

        targetRecordId = savedRecord.getId();
        RecordReactionType reactionType = RecordReactionType.GREAT;

        // 5. 카운트 테이블 로우 생성 후 DB 반영
        reactionCountRepository.saveAndFlush(ActivityRecordReactionCount.init(targetRecordId, reactionType));


        for (int i = 0; i < threadCount; i++) {
            User mockUser = userRepository.saveAndFlush(User.builder()
                    .socialId("mock_user_reaction_" + i)
                    .nickname("반응유저" + i)
                    .role(Role.USER)
                    .socialType(SocialType.KAKAO)
                    .deleted(false)
                    .build());
            mockUsers.add(mockUser);
        }

        entityManager.clear();
    }

    @Test
    @DisplayName("DB 행 잠금 검증 - 동시에 30명의 유저가 반응을 남겨도 카운트가 유실 없이 정확하게 반영된다")
    void reactToRecordConcurrencyTest() throws InterruptedException {
        RecordReactionType reactionType = RecordReactionType.GREAT;
        ExecutorService executorService = Executors.newFixedThreadPool(16);
        CountDownLatch latch = new CountDownLatch(threadCount);

        // ==================== [WHEN: 이미 검증/커밋 완료된 데이터를 기반으로 동시 요청] ====================
        for (int i = 0; i < threadCount; i++) {
            User existingUser = mockUsers.get(i);
            CustomUserDetails userDetails = new CustomUserDetails(existingUser);

            executorService.submit(() -> {
                try {
                    reactionService.reactToRecord(targetRecordId, reactionType, userDetails);
                } catch (Exception e) {
                    System.err.println("[동시요청 에러 발생] " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        entityManager.clear();

        // ==================== [THEN: 정합성 및 락 제어 정상 동작 검증] ====================
        ActivityRecordReactionCount updatedCount = reactionCountRepository.findById(targetRecordId)
                .orElseThrow(() -> new AssertionError("카운트 데이터가 존재하지 않습니다."));

        int expectedCount = threadCount + 1;

        assertThat(updatedCount.getTotalCount()).isEqualTo(expectedCount);
        assertThat(updatedCount.getGreatCount()).isEqualTo(expectedCount);
    }
}