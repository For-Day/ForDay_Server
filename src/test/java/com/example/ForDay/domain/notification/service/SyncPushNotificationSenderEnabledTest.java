package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.notification.repository.NotificationRepository;
import com.example.ForDay.domain.record.controller.v1.TestReactionMeasurementController;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.global.firebase.entity.FcmToken;
import com.example.ForDay.global.firebase.repository.FcmTokenRepository;
import com.example.ForDay.global.firebase.type.DeviceType;
import com.example.ForDay.global.port.PushSenderPort;
import com.example.ForDay.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@code measure} 프로파일이 켜졌을 때 measure 슬라이스({@link SyncPushNotificationSender},
 * {@link TestReactionMeasurementController})가 실제로 조립되는지 검증한다.
 *
 * <p>{@code @ActiveProfiles("measure")}는 {@code inheritProfiles}가 기본값 {@code true}라
 * {@link IntegrationTestSupport}의 {@code @ActiveProfiles("test")}와 합쳐져
 * {@code ["test", "measure"]}로 뜬다.
 *
 * <p>{@link PushSenderPort}는 실제 FCM 어댑터 대신 목으로 교체한다 — 여기서 검증할 것은
 * "동기 경로가 조립되고 끝까지 실행되는가"이지, 실제 FCM 발송 성공 여부가 아니다.
 */
@ActiveProfiles("measure")
class SyncPushNotificationSenderEnabledTest extends IntegrationTestSupport {

    @MockitoBean
    private PushSenderPort pushSenderPort;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private FcmTokenRepository fcmTokenRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager em;

    private String receiverId;
    private String senderId;

    @Test
    @DisplayName("SyncPushNotificationSender와 측정용 컨트롤러 빈이 등록된다")
    void measure_슬라이스가_조립된다() {
        assertThat(applicationContext.getBeanNamesForType(SyncPushNotificationSender.class)).isNotEmpty();
        assertThat(applicationContext.getBeanNamesForType(TestReactionMeasurementController.class)).isNotEmpty();
    }

    @Test
    @DisplayName("동기 발송 경로가 끝까지 실행되어 알림 저장과 FCM 발송이 일어난다")
    void 동기_발송_경로가_끝까지_실행된다() {
        User receiver = userRepository.saveAndFlush(User.builder()
                .socialId("measure_on_receiver")
                .nickname("받는사람")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .isRecordPushEnabled(true)
                .build());
        receiverId = receiver.getId();
        fcmTokenRepository.saveAndFlush(FcmToken.createFcmToken(
                "measure-device", receiver, "measure-on-fcm-token", DeviceType.ANDROID));
        User sender = userRepository.saveAndFlush(User.builder()
                .socialId("measure_on_sender")
                .nickname("보내는사람")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .build());
        senderId = sender.getId();

        long before = notificationRepository.count();

        notificationService.testProcessReactionNotification(
                sender, receiver, RecordReactionType.AMAZING, 1L, "https://example.com/image.jpg");

        assertThat(notificationRepository.count()).isEqualTo(before + 1);
        verify(pushSenderPort, times(1)).send(any());
    }

    /**
     * repository 메서드는 각각 자기 트랜잭션에서 즉시 커밋된다(이 클래스에 {@code @Transactional}이
     * 없으므로 롤백되지 않는다). {@code NotificationServiceTest}처럼 notifications 테이블을
     * 전부 스캔하는 다른 테스트가 이 클래스 뒤에 실행되면 남은 행 때문에 깨질 수 있어 직접 지운다.
     */
    @AfterEach
    void tearDown() {
        if (receiverId == null) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> {
            em.createQuery("delete from Notification n where n.receiver.id = :userId")
                    .setParameter("userId", receiverId).executeUpdate();
            em.createQuery("delete from FcmToken f where f.user.id = :userId")
                    .setParameter("userId", receiverId).executeUpdate();
            em.createQuery("delete from User u where u.id in :userIds")
                    .setParameter("userIds", List.of(receiverId, senderId)).executeUpdate();
        });
    }
}
