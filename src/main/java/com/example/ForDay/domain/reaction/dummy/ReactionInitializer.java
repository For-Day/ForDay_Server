package com.example.ForDay.domain.reaction.dummy;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.reaction.entity.ActivityRecordReaction;
import com.example.ForDay.domain.reaction.entity.ActivityRecordReactionCount;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionCountRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

//@Component
@RequiredArgsConstructor
@Profile("local")
public class ReactionInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ActivityRecordRepository recordRepository;
    private final ActivityRecordReactionRepository reactionRepository;
    private final ActivityRecordReactionCountRepository countRepository;
    private final ActivityRepository activityRepository;
    private final HobbyRepository hobbyRepository;

    @Override
    @Transactional
    public void run(String... args) {
        init();
    }

    private void init() {
        // 유저 20명 생성
        List<User> users = new ArrayList<>();

        for (int i = 1; i <= 20; i++) {
            User user = User.builder()
                    .nickname("user" + i)
                    .socialId("social_" + i)
                    .role(Role.USER)
                    .socialType(SocialType.GUEST)
                    .build();

            users.add(user);
        }

        userRepository.saveAll(users);

        // 작성자 선정
        User writer = users.get(0);

        Hobby hobby = Hobby.builder()
                .user(writer)
                .hobbyInfoId(1L)
                .hobbyPurpose("목적")
                .hobbyTimeMinutes(1)
                .executionCount(4)
                .status(HobbyStatus.IN_PROGRESS)
                .hobbyName("취미")
                .build();
        hobbyRepository.save(hobby);

        Activity activity = Activity.builder()
                .user(writer)
                .hobby(hobby)
                .content("내용")
                .aiRecommended(false)
                .build();
        activityRepository.save(activity);

        ActivityRecord record = ActivityRecord.builder()
                .user(writer)
                .memo("더미 기록입니다")
                .sticker("🔥")
                .activity(activity)
                .hobby(hobby)
                .visibility(com.example.ForDay.domain.record.type.RecordVisibility.PUBLIC)
                .build();

        recordRepository.save(record);

        // 반응 생성
        Random random = new Random();

        Map<RecordReactionType, Long> counter = new EnumMap<>(RecordReactionType.class);
        for (RecordReactionType type : RecordReactionType.values()) {
            counter.put(type, 0L);
        }

        long totalCount = 0;

        for (int i = 1; i < users.size(); i++) {
            User user = users.get(i);

            int reactionCount = random.nextInt(3) + 1;

            for (int j = 0; j < reactionCount; j++) {
                RecordReactionType type =
                        RecordReactionType.values()[random.nextInt(RecordReactionType.values().length)];

                ActivityRecordReaction reaction = ActivityRecordReaction.builder()
                        .activityRecord(record)
                        .reactedUser(user)
                        .reactionType(type)
                        .build();

                reactionRepository.save(reaction);

                // 카운트 증가
                counter.put(type, counter.get(type) + 1);
                totalCount++;
            }
        }

        ActivityRecordReactionCount count = ActivityRecordReactionCount.builder()
                .recordId(record.getId())
                .totalCount(totalCount)
                .awesomeCount(counter.getOrDefault(RecordReactionType.AWESOME, 0L))
                .greatCount(counter.getOrDefault(RecordReactionType.GREAT, 0L))
                .amazingCount(counter.getOrDefault(RecordReactionType.AMAZING, 0L))
                .fightingCount(counter.getOrDefault(RecordReactionType.FIGHTING, 0L))
                .build();

        countRepository.save(count);
    }
}