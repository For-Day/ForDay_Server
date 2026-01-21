package com.example.ForDay.domain.hobby;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.entity.ActivityRecord;
import com.example.ForDay.domain.activity.repository.ActivityRecordRepository;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.activity.type.RecordVisibility;
import com.example.ForDay.domain.hobby.dto.response.GetStickerInfoResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.service.HobbyService;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.RedisUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class GetStickerInfoServiceTodayNotRecordedTest {

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
    private RedisUtil redisUtil;

    @Test
    void 스티커_1개_오늘기록안함_전체페이지1_현재페이지null() {
        TestContext ctx = setupTestData(66, "독서", "성장", 30);

        createStickers(ctx.activity, ctx.hobby, ctx.user, 1);

        given(redisUtil.hasKey(any())).willReturn(false); // 오늘 기록 없음

        GetStickerInfoResDto result = hobbyService.getStickerInfo(ctx.hobby.getId(), null, 28, ctx.userDetails);

        assertThat(result.isDurationSet()).isTrue();
        assertThat(result.isActivityRecordedToday()).isFalse();

        assertThat(result.getCurrentPage()).isEqualTo(1);
        assertThat(result.getTotalPage()).isEqualTo(1);

        assertThat(result.isHasPrevious()).isFalse();
        assertThat(result.isHasNext()).isFalse();

        assertThat(result.getTotalStickerNum()).isEqualTo(1);
        assertThat(result.getStickers()).hasSize(1);
    }

    @Test
    void 스티커_27개_오늘기록안함_전체페이지1_현재페이지null() {
        TestContext ctx = setupTestData(66, "독서", "성장", 30);

        createStickers(ctx.activity, ctx.hobby, ctx.user, 27);

        given(redisUtil.hasKey(any())).willReturn(false);

        GetStickerInfoResDto result = hobbyService.getStickerInfo(ctx.hobby.getId(), null, 28, ctx.userDetails);

        assertThat(result.isDurationSet()).isTrue();
        assertThat(result.isActivityRecordedToday()).isFalse();

        assertThat(result.getCurrentPage()).isEqualTo(1);
        assertThat(result.getTotalPage()).isEqualTo(1);

        assertThat(result.isHasPrevious()).isFalse();
        assertThat(result.isHasNext()).isFalse();

        assertThat(result.getTotalStickerNum()).isEqualTo(27);
        assertThat(result.getStickers()).hasSize(27);
    }

    @Test
    void 스티커_28개_오늘기록안함_전체페이지2_현재페이지null() {
        TestContext ctx = setupTestData(66, "독서", "성장", 30);

        createStickers(ctx.activity, ctx.hobby, ctx.user, 28);

        given(redisUtil.hasKey(any())).willReturn(false);

        GetStickerInfoResDto result = hobbyService.getStickerInfo(ctx.hobby.getId(), null, 28, ctx.userDetails);

        assertThat(result.isDurationSet()).isTrue();
        assertThat(result.isActivityRecordedToday()).isFalse();

        assertThat(result.getCurrentPage()).isEqualTo(2);
        assertThat(result.getTotalPage()).isEqualTo(2);

        assertThat(result.isHasPrevious()).isTrue();
        assertThat(result.isHasNext()).isFalse();

        assertThat(result.getTotalStickerNum()).isEqualTo(28);
        assertThat(result.getStickers()).hasSize(0);
    }

    @Test
    void 스티커_30개_오늘기록안함_전체페이지2_현재페이지null() {
        TestContext ctx = setupTestData(66, "독서", "성장", 30);

        createStickers(ctx.activity, ctx.hobby, ctx.user, 30);

        given(redisUtil.hasKey(any())).willReturn(false);

        GetStickerInfoResDto result = hobbyService.getStickerInfo(ctx.hobby.getId(), null, 28, ctx.userDetails);

        assertThat(result.isDurationSet()).isTrue();
        assertThat(result.isActivityRecordedToday()).isFalse();

        assertThat(result.getCurrentPage()).isEqualTo(2);
        assertThat(result.getTotalPage()).isEqualTo(2);

        assertThat(result.isHasPrevious()).isTrue();
        assertThat(result.isHasNext()).isFalse();

        assertThat(result.getTotalStickerNum()).isEqualTo(30);
        assertThat(result.getStickers()).hasSize(2);
    }

    @Test
    void 스티커_56개_오늘기록안함_전체페이지3_현재페이지null() {
        TestContext ctx = setupTestData(66, "독서", "성장", 30);

        createStickers(ctx.activity, ctx.hobby, ctx.user, 56);

        given(redisUtil.hasKey(any())).willReturn(false);

        GetStickerInfoResDto result = hobbyService.getStickerInfo(ctx.hobby.getId(), null, 28, ctx.userDetails);

        assertThat(result.isDurationSet()).isTrue();
        assertThat(result.isActivityRecordedToday()).isFalse();

        assertThat(result.getCurrentPage()).isEqualTo(3);
        assertThat(result.getTotalPage()).isEqualTo(3);

        assertThat(result.isHasPrevious()).isTrue();
        assertThat(result.isHasNext()).isFalse();

        assertThat(result.getStickers()).hasSize(0);
    }

    @Test
    void 스티커_56개_오늘기록안함_전체페이지3_현재페이지2() {
        TestContext ctx = setupTestData(66, "독서", "성장", 30);

        createStickers(ctx.activity, ctx.hobby, ctx.user, 56);

        given(redisUtil.hasKey(any())).willReturn(false);

        GetStickerInfoResDto result = hobbyService.getStickerInfo(ctx.hobby.getId(), 2, 28, ctx.userDetails);

        assertThat(result.isDurationSet()).isTrue();
        assertThat(result.isActivityRecordedToday()).isFalse();

        assertThat(result.getCurrentPage()).isEqualTo(2);
        assertThat(result.getTotalPage()).isEqualTo(3);

        assertThat(result.isHasPrevious()).isTrue();
        assertThat(result.isHasNext()).isTrue();

        assertThat(result.getStickers()).hasSize(28);
    }

    @Test
    void 스티커_66개_오늘기록안함_전체페이지3_현재페이지null() {
        TestContext ctx = setupTestData(66, "독서", "성장", 30);

        createStickers(ctx.activity, ctx.hobby, ctx.user, 66);

        given(redisUtil.hasKey(any())).willReturn(false); // durationSet = false

        GetStickerInfoResDto result = hobbyService.getStickerInfo(ctx.hobby.getId(), null, 28, ctx.userDetails);

        assertThat(result.isDurationSet()).isTrue();
        assertThat(result.isActivityRecordedToday()).isFalse();

        assertThat(result.getCurrentPage()).isEqualTo(3);
        assertThat(result.getTotalPage()).isEqualTo(3);

        assertThat(result.isHasPrevious()).isTrue();
        assertThat(result.isHasNext()).isFalse();

        assertThat(result.getStickers()).hasSize(10);
    }

    @Test
    void 스티커_66개_오늘기록안함_전체페이지3_현재페이지1() {
        TestContext ctx = setupTestData(66, "독서", "성장", 30);

        createStickers(ctx.activity, ctx.hobby, ctx.user, 66);

        given(redisUtil.hasKey(any())).willReturn(false); // durationSet = false

        GetStickerInfoResDto result = hobbyService.getStickerInfo(ctx.hobby.getId(), 1, 28, ctx.userDetails);

        assertThat(result.isDurationSet()).isTrue();
        assertThat(result.isActivityRecordedToday()).isFalse();

        assertThat(result.getCurrentPage()).isEqualTo(1);
        assertThat(result.getTotalPage()).isEqualTo(3);

        assertThat(result.isHasPrevious()).isFalse();
        assertThat(result.isHasNext()).isTrue();

        assertThat(result.getStickers()).hasSize(28);
    }



    private class TestContext {
        User user;
        CustomUserDetails userDetails;
        Hobby hobby;
        Activity activity;
    }

    private TestContext setupTestData(int goalDays, String hobbyName, String hobbyPurpose, int hobbyTimeMinutes) {
        TestContext ctx = new TestContext();

        ctx.user = userRepository.save(User.builder()
                .role(Role.GUEST)
                .socialType(SocialType.GUEST)
                .socialId("test-user")
                .build());

        ctx.userDetails = new CustomUserDetails(ctx.user);

        ctx.hobby = hobbyRepository.save(Hobby.builder()
                .user(ctx.user)
                .hobbyName(hobbyName)
                .hobbyPurpose(hobbyPurpose)
                .hobbyTimeMinutes(hobbyTimeMinutes)
                .executionCount(0)
                .goalDays(goalDays)
                .status(HobbyStatus.IN_PROGRESS)
                .build());

        ctx.activity = activityRepository.save(Activity.builder()
                .user(ctx.user)
                .hobby(ctx.hobby)
                .content("테스트 활동")
                .build());

        return ctx;
    }

    private void createStickers(Activity activity, Hobby hobby, User user, int count) {
        for (int i = 0; i < count; i++) {
            activity.record();
            activityRecordRepository.save(ActivityRecord.builder()
                    .activity(activity)
                    .hobby(hobby)
                    .user(user)
                    .sticker("smile")
                    .visibility(RecordVisibility.PUBLIC)
                    .build());
        }
    }

}
