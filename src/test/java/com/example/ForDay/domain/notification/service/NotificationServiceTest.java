package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.notification.entity.ReactionNotification;
import com.example.ForDay.domain.notification.repository.NotificationRepository;
import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HobbyRepository hobbyRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ActivityRecordRepository recordRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private EntityManager em;

    @BeforeEach
    void setUp() {
        // 받는 사람
        User receiver = User.builder()
                .email("받는 사람 이메일")
                .nickname("받는 사람 닉네임")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("user_받는 사람")
                .profileImageUrl("받는 사람 프로필 url")
                .build();
        userRepository.save(receiver);

        // 보내는 사람
        User sender = User.builder()
                .email("보내는 사람 이메일")
                .nickname("보내는 사람 닉네임")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("user_보내는 사람")
                .profileImageUrl("보내는 사람 프로필 url")
                .build();
        userRepository.save(sender);

        // 취미
        Hobby hobby = Hobby.builder()
                .user(receiver)
                .hobbyInfoId(1L)
                .hobbyName("그림")
                .hobbyPurpose("목적")
                .hobbyTimeMinutes(10)
                .executionCount(4)
                .status(HobbyStatus.IN_PROGRESS)
                .build();
        hobbyRepository.save(hobby);

        // 활동
        Activity activity = Activity.builder()
                .user(receiver)
                .hobby(hobby)
                .content("활동 내용")
                .aiRecommended(true)
                .build();
        activityRepository.save(activity);

        // 기록
        ActivityRecord record = ActivityRecord.builder()
                .activity(activity)
                .hobby(hobby)
                .user(receiver)
                .sticker("smile.jpg")
                .memo("메모")
                .visibility(RecordVisibility.PUBLIC)
                .imageUrl("기록 이미지 url")
                .build();
        recordRepository.save(record);

        // 알림 ReactionNotification
        ReactionNotification reactionNotification1 =
                ReactionNotification.create(receiver, sender, NotificationType.RECORD_REACTION, "알림 메세지1", RecordReactionType.AMAZING, record.getId(), record.getImageUrl());
        ReactionNotification reactionNotification2 =
                ReactionNotification.create(receiver, sender, NotificationType.RECORD_REACTION, "알림 메세지2", RecordReactionType.GREAT, record.getId(), record.getImageUrl());
        ReactionNotification reactionNotification3 =
                ReactionNotification.create(receiver, sender, NotificationType.RECORD_REACTION, "알림 메세지3", RecordReactionType.AWESOME, record.getId(), record.getImageUrl());

        notificationRepository.save(reactionNotification1);
        notificationRepository.save(reactionNotification2);
        notificationRepository.save(reactionNotification3);
    }

    @Test
    void 기록_이미지_업데이트시_notification_imageUrl_변경_확인() {
        // given
        ActivityRecord record = recordRepository.findAll().get(0);
        String newImageUrl = "https://new-image-url.com/image.jpg";

        // when
        notificationRepository.updateImageUrlByRecordId(record.getId(), newImageUrl);

        em.flush();
        em.clear();

        // then
        var notifications = notificationRepository.findAll();

        for (var notification : notifications) {
            if (notification instanceof ReactionNotification) {
                org.assertj.core.api.Assertions.assertThat(((ReactionNotification) notification).getImageUrl())
                        .isEqualTo(newImageUrl);
            }
        }
    }

    @Test
    void 기록_삭제시_연관된_notification_imageUrl이_null로_변경되는지_확인() {
        // given
        ActivityRecord record = recordRepository.findAll().get(0);
        Long recordId = record.getId();

        // when
        notificationRepository.updateImageUrlByRecordId(recordId, null);

        em.flush();
        em.clear();

        // then
        var notifications = notificationRepository.findAll();

        org.assertj.core.api.Assertions.assertThat(notifications).isNotEmpty();
        for (var notification : notifications) {
            if (notification instanceof ReactionNotification) {
                org.assertj.core.api.Assertions.assertThat(((ReactionNotification) notification).getImageUrl())
                        .isNull();
            }
        }
    }


}