package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
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
import com.example.ForDay.global.rabbitmq.config.RabbitMqConfig;
import com.example.ForDay.global.rabbitmq.dto.NotificationEventDto;
import com.example.ForDay.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 알림 발행이 트랜잭션 커밋에 묶여 있다는 것을 고정하는 테스트.
 *
 * <p>{@code NotificationEventListener}가 {@code @TransactionalEventListener(AFTER_COMMIT)}로
 * RabbitMQ에 발행하므로 "DB는 롤백됐는데 알림은 나가는" 상황이 구조적으로 막혀 있다.
 * 그 동작을 테스트로 고정해, 누가 phase를 지우거나 평범한 {@code @EventListener}로 바꾸면
 * CI가 잡도록 한다.
 *
 * <p><b>이 클래스에 {@code @Transactional}을 붙이면 안 된다.</b> 테스트가 트랜잭션을 잡고
 * 항상 롤백하면 AFTER_COMMIT 리스너가 아예 실행되지 않아, "커밋되면 발행된다" 쪽을
 * 증명할 수 없다. 그래서 실제로 커밋되는 방식으로 두고 데이터는 {@link #tearDown()}에서
 * 직접 지운다.
 *
 * <p>{@link RabbitTemplate}은 스파이가 아니라 목으로 교체한다. 스파이면 실제 발행이 일어나
 * RabbitMQ가 없는 테스트 환경에서 커넥션 예외가 발생하는데, AFTER_COMMIT 리스너에서 난
 * 예외는 스프링이 삼키므로 테스트는 통과하면서 로그만 지저분해진다. 목이면 커넥션 없이
 * 발행 횟수만 정확히 셀 수 있다.
 *
 * <p>대상은 v1 경로({@code POST /records/{id}/reaction} → {@code ReactionService#reactToRecord})다.
 * v2 경로({@code reactToRecordWithRedis})는 Write-Back 부하 측정용이라 알림을 발행하지 않는다.
 */
class NotificationTransactionalPublishTest extends IntegrationTestSupport {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

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
        // 빈 리스트를 돌려주고 이벤트 자체가 발행되지 않는다. 반드시 true로 만든다.
        User receiver = userRepository.saveAndFlush(User.builder()
                .socialId("tx_publish_receiver")
                .nickname("기록작성자")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .isRecordPushEnabled(true)
                .deleted(false)
                .build());
        receiverId = receiver.getId();

        // 같은 이유로 FCM 토큰이 최소 1건 있어야 발행 조건이 성립한다.
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
     */
    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            em.createQuery("delete from Notification n where n.receiver.id = :userId")
                    .setParameter("userId", receiverId).executeUpdate();
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

        @Test
        @DisplayName("RabbitMQ로 알림이 발행되지 않는다")
        void 알림이_발행되지_않는다() {
            // when: 리액션 등록이 성공한 뒤 같은 트랜잭션에서 예외가 터진다
            assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
                reactionService.reactToRecord(recordId, RecordReactionType.AMAZING, senderDetails);
                throw new IllegalStateException("트랜잭션 롤백 유도");
            })).isInstanceOf(IllegalStateException.class);

            // then: AFTER_COMMIT 리스너가 실행되지 않아 발행 0건
            verify(rabbitTemplate, never()).convertAndSend(
                    eq(RabbitMqConfig.NOTIFICATION_EXCHANGE),
                    eq(RabbitMqConfig.NOTIFICATION_ROUTING_KEY),
                    any(NotificationEventDto.class));
        }

        @Test
        @DisplayName("알림도 저장되지 않는다")
        void 알림이_저장되지_않는다() {
            assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
                reactionService.reactToRecord(recordId, RecordReactionType.AMAZING, senderDetails);
                throw new IllegalStateException("트랜잭션 롤백 유도");
            })).isInstanceOf(IllegalStateException.class);

            assertThat(countNotificationsForReceiver()).isZero();
        }
    }

    @Nested
    @DisplayName("리액션 트랜잭션이 커밋되면")
    class WhenCommitted {

        @Test
        @DisplayName("알림 저장 건수와 RabbitMQ 발행 건수가 일치한다")
        void 저장_건수와_발행_건수가_같다() {
            // when: 자체 트랜잭션에서 커밋된다
            reactionService.reactToRecord(recordId, RecordReactionType.AMAZING, senderDetails);

            // then: 저장 1건 ↔ 발행 1건
            long savedCount = countNotificationsForReceiver();
            assertThat(savedCount).isEqualTo(1L);

            verify(rabbitTemplate, times((int) savedCount)).convertAndSend(
                    eq(RabbitMqConfig.NOTIFICATION_EXCHANGE),
                    eq(RabbitMqConfig.NOTIFICATION_ROUTING_KEY),
                    any(NotificationEventDto.class));
        }
    }

    /**
     * {@code Notification.receiver}가 LAZY라서 트랜잭션 밖에서 프록시를 건드리면 깨진다.
     * 카운트 쿼리로 세면 프록시를 초기화하지 않고, 다른 테스트가 남긴 행도 섞이지 않는다.
     */
    private long countNotificationsForReceiver() {
        return em.createQuery(
                        "select count(n) from Notification n where n.receiver.id = :userId", Long.class)
                .setParameter("userId", receiverId)
                .getSingleResult();
    }
}
