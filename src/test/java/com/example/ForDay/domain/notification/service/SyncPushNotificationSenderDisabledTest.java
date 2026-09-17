package com.example.ForDay.domain.notification.service;

import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 기본 프로파일({@code test}, {@code measure}를 켜지 않음)에서 {@code measure} 슬라이스가
 * 조립되지 않는다는 것을 고정한다. 이 클래스는 {@link IntegrationTestSupport}의
 * {@code @ActiveProfiles("test")}를 그대로 쓴다.
 *
 * <p>measure 프로파일 ON 쪽 검증은 {@link SyncPushNotificationSenderEnabledTest} 참고.
 */
class SyncPushNotificationSenderDisabledTest extends IntegrationTestSupport {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @DisplayName("SyncPushNotificationSender 빈이 등록되지 않는다")
    void 빈이_등록되지_않는다() {
        assertThat(applicationContext.getBeanNamesForType(SyncPushNotificationSender.class)).isEmpty();
    }

    @Test
    @DisplayName("measure 프로파일 전용 컨트롤러도 등록되지 않는다")
    void 측정용_컨트롤러도_등록되지_않는다() {
        assertThat(applicationContext.getBeanNamesForType(
                com.example.ForDay.domain.record.controller.v1.TestReactionMeasurementController.class))
                .isEmpty();
    }

    @Test
    @DisplayName("testProcessReactionNotification 호출 시 IllegalStateException을 던진다")
    void 동기_발송을_호출하면_예외가_발생한다() {
        // 가드가 메서드 첫 줄이라 DB에 저장할 필요조차 없다 - 영속화 전에 던진다.
        User sender = User.builder()
                .socialId("guard_sender")
                .nickname("보내는사람")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .build();
        User receiver = User.builder()
                .socialId("guard_receiver")
                .nickname("받는사람")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .isRecordPushEnabled(true)
                .build();

        assertThatThrownBy(() -> notificationService.testProcessReactionNotification(
                sender, receiver, RecordReactionType.AMAZING, 1L, "https://example.com/image.jpg"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("measure");
    }
}
