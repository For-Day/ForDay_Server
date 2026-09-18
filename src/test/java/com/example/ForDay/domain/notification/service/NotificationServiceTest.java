package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.notification.document.NotificationDocument;
import com.example.ForDay.domain.notification.repository.NotificationDocumentRepository;
import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.global.mongo.service.MongoSequenceGeneratorService;
import com.example.ForDay.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이슈 #408 - 알림 도메인이 MongoDB로 옮겨간 뒤의 {@code updateImageUrlByRecordId} 동작을
 * 검증한다. {@code notifications} 컬렉션은 JPA {@code @Transactional} 롤백 대상이 아니라서
 * (별도 저장소) 이 클래스에 {@code @Transactional}을 붙여도 Mongo 쪽 데이터는 롤백되지
 * 않는다 - {@link #tearDown()}에서 직접 지운다. {@code @Transactional}은 MySQL 쪽
 * 픽스처(User/Hobby/Activity/ActivityRecord)만 롤백하기 위해 유지한다.
 */
@Transactional
class NotificationServiceTest extends IntegrationTestSupport {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HobbyRepository hobbyRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ActivityRecordRepository recordRepository;

    @Autowired
    private NotificationDocumentRepository notificationDocumentRepository;

    @Autowired
    private MongoSequenceGeneratorService sequenceGeneratorService;

    private Long recordId;
    private final List<Long> createdNotificationIds = new java.util.ArrayList<>();

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
        recordId = record.getId();

        // 알림 3건 (전부 같은 기록을 가리킴)
        saveReactionNotification(receiver, sender, "알림 메세지1", record.getImageUrl());
        saveReactionNotification(receiver, sender, "알림 메세지2", record.getImageUrl());
        saveReactionNotification(receiver, sender, "알림 메세지3", record.getImageUrl());
    }

    @AfterEach
    void tearDown() {
        createdNotificationIds.forEach(notificationDocumentRepository::deleteById);
    }

    private void saveReactionNotification(User receiver, User sender, String message, String imageUrl) {
        long id = sequenceGeneratorService.generateSequence(NotificationDocument.SEQUENCE_NAME);
        NotificationDocument document = NotificationDocument.create(
                id, receiver.getId(), sender.getId(), sender.getProfileImageUrl(),
                NotificationType.RECORD_REACTION, message, imageUrl,
                Map.of("reactionType", "GREAT", "recordId", recordId));
        notificationDocumentRepository.save(document);
        createdNotificationIds.add(id);
    }

    @Test
    void 기록_이미지_업데이트시_notification_imageUrl_변경_확인() {
        // given
        String newImageUrl = "https://new-image-url.com/image.jpg";

        // when
        notificationDocumentRepository.updateImageUrlByRecordId(recordId, newImageUrl);

        // then
        List<NotificationDocument> notifications = notificationDocumentRepository.findAllById(createdNotificationIds);
        assertThat(notifications).hasSize(3);
        notifications.forEach(n -> assertThat(n.getImageUrl()).isEqualTo(newImageUrl));
    }

    @Test
    void 기록_삭제시_연관된_notification_imageUrl이_null로_변경되는지_확인() {
        // when
        notificationDocumentRepository.updateImageUrlByRecordId(recordId, null);

        // then
        List<NotificationDocument> notifications = notificationDocumentRepository.findAllById(createdNotificationIds);
        assertThat(notifications).hasSize(3);
        notifications.forEach(n -> assertThat(n.getImageUrl()).isNull());
    }
}
