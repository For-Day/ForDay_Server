package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.notification.entity.NotificationOutbox;
import com.example.ForDay.domain.notification.repository.NotificationDocumentRepository;
import com.example.ForDay.domain.notification.repository.NotificationOutboxRepository;
import com.example.ForDay.domain.notification.type.OutboxStatus;
import com.example.ForDay.domain.reaction.service.ReactionService;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.global.firebase.entity.FcmToken;
import com.example.ForDay.global.firebase.repository.FcmTokenRepository;
import com.example.ForDay.global.firebase.type.DeviceType;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 알림 저장(MongoDB)과 outbox 저장(MySQL)이 정합성 있게 맞물려 있다는 것을 고정하는 테스트.
 *
 * <p>이슈 #408 이전에는 {@code Notification}과 {@code NotificationOutbox}를 같은 MySQL
 * 트랜잭션 안에서 함께 저장해 원자성이 구조적으로 보장됐다. MongoDB(standalone)로 옮긴
 * 뒤로는 다중 문서 트랜잭션이 없어 두 저장을 하나로 묶을 수 없고, 대신
 * {@code NotificationService#processReactionNotification}이 Mongo 저장을 먼저 하고
 * 실패하면 예외를 던져 감싸고 있는 리액션 트랜잭션(Outbox 포함)을 롤백시키는 방식으로
 * "알림 없이 outbox만 남는" 상황을 막는다. 반대 방향(Mongo 저장 성공 후 리액션 트랜잭션이
 * 롤백되는 경우)은 이 테스트가 검증하지 않는다 - 이슈 #408에서 허용하기로 한 트레이드오프.
 * 실제 RabbitMQ 발행은 이 트랜잭션과 완전히 분리된 {@link NotificationOutboxRelay}가
 * 맡는다 — 그쪽 동작은 {@link NotificationOutboxRelayTest}에서 검증한다.
 *
 * <p><b>이 클래스에 {@code @Transactional}을 붙이지 않는다.</b> #370 당시엔 AFTER_COMMIT
 * 리스너를 실행시키기 위해 필요했던 제약인데, Outbox 도입 후에는 발행이 완전히 비동기라
 * 이 제약이 더는 필수는 아니다. 다만 기존 테스트 스타일과의 일관성을 위해 유지하고,
 * 커밋된 데이터는 {@link #tearDown()}에서 직접 지운다.
 *
 * <p>대상은 v1 경로({@code POST /records/{id}/reaction} → {@code ReactionService#reactToRecord})다.
 * v2 경로({@code reactToRecordWithRedis})는 Write-Back 부하 측정용이라 알림을 발행하지 않는다.
 */
class NotificationTransactionalPublishTest extends IntegrationTestSupport {

    @Autowired
    private ReactionService reactionService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HobbyRepository hobbyRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ActivityRecordRepository activityRecordRepository;

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @Autowired
    private NotificationOutboxRepository notificationOutboxRepository;

    @Autowired
    private NotificationDocumentRepository notificationDocumentRepository;

    @Autowired
    private EntityManager em;

    private String receiverId;
    private String senderId;
    private Long hobbyId;
    private Long activityId;
    private Long recordId;
    private CustomUserDetails senderDetails;

    @BeforeEach
    void setUp() {
        // 기록 작성자 = 알림 받는 사람.
        // isRecordPushEnabled 기본값이 false인데, false면 findActiveRecordDeviceToken이
        // 빈 리스트를 돌려주고 outbox 행 자체가 생기지 않는다. 반드시 true로 만든다.
        User receiver = userRepository.saveAndFlush(User.builder()
                .socialId("tx_publish_receiver")
                .nickname("기록작성자")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .isRecordPushEnabled(true)
                .deleted(false)
                .build());
        receiverId = receiver.getId();

        // 같은 이유로 FCM 토큰이 최소 1건 있어야 outbox 저장 조건이 성립한다.
        fcmTokenRepository.saveAndFlush(FcmToken.createFcmToken(
                "tx-publish-device", receiver, "tx-publish-fcm-token", DeviceType.ANDROID));

        User sender = userRepository.saveAndFlush(User.builder()
                .socialId("tx_publish_sender")
                .nickname("반응유저")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .deleted(false)
                .build());
        senderId = sender.getId();
        senderDetails = new CustomUserDetails(sender);

        Hobby hobby = hobbyRepository.saveAndFlush(Hobby.builder()
                .user(receiver)
                .hobbyName("매일 운동하기")
                .hobbyPurpose("체력 증진")
                .hobbyTimeMinutes(45)
                .executionCount(1)
                .status(HobbyStatus.IN_PROGRESS)
                .build());
        hobbyId = hobby.getId();

        Activity activity = activityRepository.saveAndFlush(Activity.builder()
                .user(receiver)
                .hobby(hobby)
                .content("오늘 오운완 성공했습니다!")
                .build());
        activityId = activity.getId();

        // 남이 반응할 수 있어야 하므로 PUBLIC. 작성자 본인이 반응하면 알림을 보내지 않는다.
        ActivityRecord record = activityRecordRepository.saveAndFlush(ActivityRecord.builder()
                .activity(activity)
                .hobby(hobby)
                .user(receiver)
                .sticker("sports_sticker")
                .memo("메모")
                .visibility(RecordVisibility.PUBLIC)
                .imageUrl("https://example.com/record.jpg")
                .build());
        recordId = record.getId();
    }

    /**
     * 이 테스트는 롤백되지 않고 실제로 커밋되므로, 만든 데이터를 FK 역순으로 직접 지운다.
     * H2는 JVM 단위로만 스키마를 새로 만들기 때문에 정리하지 않으면 뒤따르는 테스트로 샌다.
     * 알림 본문은 MongoDB(별도 저장소, MySQL 트랜잭션과 무관)에 있어 이 트랜잭션 정리와
     * 별개로 직접 지운다.
     */
    @AfterEach
    void tearDown() {
        notificationDocumentRepository.deleteByReceiverId(receiverId);
        transactionTemplate.executeWithoutResult(status -> {
            em.createQuery("delete from NotificationOutbox").executeUpdate();
            em.createQuery("delete from ActivityRecordReaction r where r.activityRecord.id = :recordId")
                    .setParameter("recordId", recordId).executeUpdate();
            em.createQuery("delete from ActivityRecordReactionCount c where c.recordId = :recordId")
                    .setParameter("recordId", recordId).executeUpdate();
            em.createQuery("delete from FcmToken f where f.user.id = :userId")
                    .setParameter("userId", receiverId).executeUpdate();
            em.createQuery("delete from ActivityRecord ar where ar.id = :recordId")
                    .setParameter("recordId", recordId).executeUpdate();
            em.createQuery("delete from Activity a where a.id = :activityId")
                    .setParameter("activityId", activityId).executeUpdate();
            em.createQuery("delete from Hobby h where h.id = :hobbyId")
                    .setParameter("hobbyId", hobbyId).executeUpdate();
            em.createQuery("delete from User u where u.id in :userIds")
                    .setParameter("userIds", List.of(receiverId, senderId)).executeUpdate();
        });
    }

    @Nested
    @DisplayName("리액션 트랜잭션이 롤백되면")
    class WhenRolledBack {

        /**
         * 이슈 #408 이전에는 Notification도 같은 MySQL 트랜잭션 안에 있어서 outbox와 함께
         * 사라졌다. MongoDB로 옮긴 뒤로는 알림 문서가 이 MySQL 트랜잭션 밖에서 이미
         * 커밋되어 있으므로, 리액션 트랜잭션이 롤백돼도 알림 문서는 그대로 남는다 - 이슈
         * #408에서 명시적으로 감수하기로 한 트레이드오프(반대 방향은 처리하지 않음)다.
         * outbox는 여전히 MySQL 안에 있으므로 정상적으로 롤백된다.
         */
        @Test
        @DisplayName("outbox 행은 생기지 않지만, 이미 저장된 알림 문서는 남는다(MongoDB는 이 트랜잭션 밖)")
        void outbox는_사라지지만_알림_문서는_남는다() {
            // when: 리액션 등록이 성공한 뒤 같은 트랜잭션에서 예외가 터진다
            assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
                reactionService.reactToRecord(recordId, RecordReactionType.AMAZING, senderDetails);
                throw new IllegalStateException("트랜잭션 롤백 유도");
            })).isInstanceOf(IllegalStateException.class);

            // then: NotificationOutbox(MySQL)는 롤백되지만 알림 문서(MongoDB)는 별도 저장소라 남는다
            assertThat(countNotificationsForReceiver()).isEqualTo(1L);
            assertThat(notificationOutboxRepository.count()).isZero();
        }
    }

    @Nested
    @DisplayName("리액션 트랜잭션이 커밋되면")
    class WhenCommitted {

        @Test
        @DisplayName("알림 저장 건수와 outbox 행 건수가 일치하고, outbox는 PENDING 상태로 남는다")
        void 저장_건수와_outbox_건수가_같고_PENDING이다() {
            // when: 자체 트랜잭션에서 커밋된다
            reactionService.reactToRecord(recordId, RecordReactionType.AMAZING, senderDetails);

            // then: 저장 1건 ↔ outbox 1건, 아직 릴레이가 안 돌았으니 PENDING
            long savedCount = countNotificationsForReceiver();
            assertThat(savedCount).isEqualTo(1L);

            List<NotificationOutbox> outboxRows = notificationOutboxRepository.findAll();
            assertThat(outboxRows).hasSize(1);
            assertThat(outboxRows.get(0).getStatus()).isEqualTo(OutboxStatus.PENDING);
            assertThat(outboxRows.get(0).getPayload()).contains("tx-publish-fcm-token");
        }
    }

    private long countNotificationsForReceiver() {
        return notificationDocumentRepository.countByReceiverId(receiverId);
    }
}
