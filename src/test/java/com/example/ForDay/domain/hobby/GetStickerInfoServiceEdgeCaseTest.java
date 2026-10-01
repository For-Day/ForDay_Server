package com.example.ForDay.domain.hobby;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.dto.response.GetStickerInfoResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.service.v1.HobbyService;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.service.TodayRecordRedisService;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@Transactional
class GetStickerInfoServiceEdgeCaseTest extends IntegrationTestSupport {

    @Autowired
    private HobbyService hobbyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HobbyRepository hobbyRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private ActivityRecordRepository activityRecordRepository;

    @MockitoBean
    private TodayRecordRedisService todayRecordRedisService;

    @Test
    void 전체페이지를_넘는_페이지_요청시_예외() {
        TestContext ctx = setupTestData(66, 1);

        given(todayRecordRedisService.hasKey(any())).willReturn(true);

        assertThatThrownBy(() -> hobbyService.getStickerInfo(ctx.hobby.getId(), 2, 28, ctx.userDetails))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PAGE_REQUEST);
    }

    @Test
    void 페이지_0_요청시_마지막페이지로_보정() {
        TestContext ctx = setupTestData(66, 30);

        given(todayRecordRedisService.hasKey(any())).willReturn(true);

        GetStickerInfoResDto result = hobbyService.getStickerInfo(ctx.hobby.getId(), 0, 28, ctx.userDetails);

        assertThat(result.getCurrentPage()).isEqualTo(2);
        assertThat(result.getTotalPage()).isEqualTo(2);
        assertThat(result.isHasPrevious()).isTrue();
        assertThat(result.isHasNext()).isFalse();
    }

    @Test
    void 기간_미설정_취미는_durationSet_false() {
        TestContext ctx = setupTestData(null, 1);

        given(todayRecordRedisService.hasKey(any())).willReturn(true);

        GetStickerInfoResDto result = hobbyService.getStickerInfo(ctx.hobby.getId(), null, 28, ctx.userDetails);

        assertThat(result.isDurationSet()).isFalse();
        assertThat(result.getTotalStickerNum()).isEqualTo(1);
    }

    @Test
    void 스티커_0개_오늘기록함_빈_슬롯없이_1페이지() {
        TestContext ctx = setupTestData(66, 0);

        given(todayRecordRedisService.hasKey(any())).willReturn(true);

        GetStickerInfoResDto result = hobbyService.getStickerInfo(ctx.hobby.getId(), null, 28, ctx.userDetails);

        assertThat(result.getCurrentPage()).isEqualTo(1);
        assertThat(result.getTotalPage()).isEqualTo(1);
        assertThat(result.isHasPrevious()).isFalse();
        assertThat(result.isHasNext()).isFalse();
        assertThat(result.getTotalStickerNum()).isZero();
        assertThat(result.getStickers()).isEmpty();
    }

    private class TestContext {
        User user;
        CustomUserDetails userDetails;
        Hobby hobby;
        Activity activity;
    }

    private TestContext setupTestData(Integer goalDays, int stickerCount) {
        TestContext ctx = new TestContext();

        ctx.user = userRepository.save(User.builder()
                .role(Role.GUEST)
                .socialType(SocialType.GUEST)
                .socialId("test-user")
                .build());

        ctx.userDetails = new CustomUserDetails(ctx.user);

        ctx.hobby = hobbyRepository.save(Hobby.builder()
                .user(ctx.user)
                .hobbyName("독서")
                .hobbyPurpose("성장")
                .hobbyTimeMinutes(30)
                .executionCount(0)
                .goalDays(goalDays)
                .status(HobbyStatus.IN_PROGRESS)
                .build());

        ctx.activity = activityRepository.save(Activity.builder()
                .user(ctx.user)
                .hobby(ctx.hobby)
                .content("테스트 활동")
                .build());

        for (int i = 0; i < stickerCount; i++) {
            ctx.activity.record();
            activityRecordRepository.save(ActivityRecord.builder()
                    .activity(ctx.activity)
                    .hobby(ctx.hobby)
                    .user(ctx.user)
                    .sticker("smile")
                    .visibility(RecordVisibility.PUBLIC)
                    .build());
        }

        return ctx;
    }
}
