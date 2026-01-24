package com.example.ForDay.domain.record.dummy;

import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.ActivityRecordReaction;
import com.example.ForDay.domain.record.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

//@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final ActivityRecordReactionRepository reactionRepository;
    private final ActivityRecordRepository recordRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        // 1. ID가 43인 활동 기록 조회
        ActivityRecord record = recordRepository.findById(43L)
                .orElseThrow(() -> new RuntimeException("ID 43인 ActivityRecord가 존재하지 않습니다."));

        // 3. 반복문을 통한 30명의 사용자 및 리액션 생성
        for (int i = 1; i <= 30; i++) {
            String nickname = "테스터" + i;
            String socialId = "test_user_" + i;

            // 유저 조회 또는 생성
            User testUser = userRepository.save(User.builder()
                    .role(Role.GUEST)
                    .socialType(SocialType.GUEST)
                    .nickname(nickname)
                    .socialId(socialId)
                    .build());

            // i값에 따라 리액션 타입을 골고루 분배 (AWESOME, GREAT, AMAZING, FIGHTING)
            RecordReactionType type = RecordReactionType.AWESOME;

            // 중복 생성 방지 체크
            boolean exists = reactionRepository.existsByActivityRecordAndReactedUserAndReactionType(
                    record, testUser, type);

            if (!exists) {
                ActivityRecordReaction reaction = ActivityRecordReaction.builder()
                        .activityRecord(record)
                        .reactedUser(testUser)
                        .reactionType(type)
                        .readWriter(false)
                        .build();

                reactionRepository.save(reaction);
            }
        }

        System.out.println("✅ 더미 데이터 생성 완료: ID 43 기록에 대해 30명의 리액션이 추가되었습니다.");
    }
}