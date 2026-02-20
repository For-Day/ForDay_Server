package com.example.ForDay.domain.record.dummy;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final HobbyRepository hobbyRepository;
    private final ActivityRepository activityRepository;
    private final ActivityRecordRepository activityRecordRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initData() {
        if (userRepository.count() >= 12) {
            log.info("이미 더미 데이터가 존재하여 생성을 건너뜁니다.");
            return;
        }

        log.info("더미 데이터 생성을 시작합니다...");

        // 1. 12명의 유저 생성 (3명씩 hobbyInfoId 1, 2, 3, 4 할당을 위해)
        for (int i = 1; i <= 12; i++) {
            User user = User.builder()
                    .email("user" + i + "@example.com")
                    .nickname("Hobbyist_" + i)
                    .role(Role.USER)
                    .socialType(SocialType.KAKAO)
                    .socialId("kakao_social_id_" + UUID.randomUUID().toString().substring(0, 8))
                    .onboardingCompleted(true)
                    .build();
            userRepository.save(user);

            // 2. 유저당 1개의 취미 생성 (hobbyInfoId: 1~4 반복 할당)
            long hobbyInfoId = ((i - 1) / 3) + 1; // 1~3번: 1, 4~6번: 2 ...
            Hobby hobby = Hobby.builder()
                    .user(user)
                    .hobbyInfoId(hobbyInfoId)
                    .hobbyName("Hobby Name " + hobbyInfoId)
                    .hobbyPurpose("Purpose of hobby " + i)
                    .hobbyTimeMinutes(30)
                    .executionCount(0)
                    .currentStickerNum(0)
                    .status(HobbyStatus.IN_PROGRESS)
                    .build();
            hobbyRepository.save(hobby);

            // 3. 취미당 1개의 활동 생성
            Activity activity = Activity.builder()
                    .user(user)
                    .hobby(hobby)
                    .content("Main Activity for " + hobby.getHobbyName())
                    .aiRecommended(false)
                    .collectedStickerNum(0)
                    .build();
            activityRepository.save(activity);

            // 4. 활동 1개당 3개의 기록 생성
            List<String> stickerImages = List.of("smile.jpg", "sad.jpg", "laugh.jpg", "angry.jpg");
            Random random = new Random();

            for (int j = 1; j <= 10; j++) {
                // 2. 리스트에서 랜덤하게 하나 추출
                String randomSticker = stickerImages.get(random.nextInt(stickerImages.size()));

                ActivityRecord record = ActivityRecord.builder()
                        .user(user)
                        .hobby(hobby)
                        .activity(activity)
                        .sticker(randomSticker) // 랜덤 이미지 적용
                        .memo("Day " + j + " record for user " + i)
                        .visibility(RecordVisibility.PUBLIC)
                        .imageUrl("https://dummy.image/record_" + i + "_" + j + ".jpg")
                        .build();

                activityRecordRepository.save(record);
            }

            // Entity의 비즈니스 로직 호출 (카운트 증가 등 연관관계 편의 로직)
            activity.record();
        }
    }
}
