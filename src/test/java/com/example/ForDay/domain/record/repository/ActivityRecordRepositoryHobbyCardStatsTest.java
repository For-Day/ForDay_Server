package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.record.dto.HobbyCardActivityStatDto;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이슈 #388 - 취미 카드 문구 생성용 QueryDSL 집계 쿼리 검증.
 * H2(MySQL 호환 모드)에서 group by + count + hour() 함수가 실제로 동작하는지가
 * 핵심 위험이라 Mockito 단위 테스트가 아니라 실제 리포지토리로 검증한다.
 */
@Transactional
class ActivityRecordRepositoryHobbyCardStatsTest extends IntegrationTestSupport {

    @Autowired private ActivityRecordRepository activityRecordRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private HobbyRepository hobbyRepository;
    @Autowired private ActivityRepository activityRepository;

    @Test
    @DisplayName("활동별 기록 수 기준으로 내림차순 정렬하고 지정한 개수만큼만 반환한다")
    void 활동별_기록_수_상위N개를_반환한다() {
        User user = userRepository.save(User.builder()
                .socialId("social_a").nickname("유저A").role(Role.USER)
                .socialType(SocialType.KAKAO).deleted(false).build());
        Hobby hobby = hobbyRepository.save(Hobby.builder()
                .user(user).hobbyName("독서").hobbyPurpose("휴식")
                .hobbyTimeMinutes(30).executionCount(3)
                .status(HobbyStatus.IN_PROGRESS).build());

        Activity mostActivity = createActivity(user, hobby, "많이 한 활동");
        Activity leastActivity = createActivity(user, hobby, "적게 한 활동");
        Activity midActivity = createActivity(user, hobby, "중간 활동");

        saveRecords(user, hobby, mostActivity, 3);
        saveRecords(user, hobby, midActivity, 2);
        saveRecords(user, hobby, leastActivity, 1);

        List<HobbyCardActivityStatDto> top2 =
                activityRecordRepository.findTopActivityStatsByHobbyId(hobby.getId(), 2);

        assertThat(top2).hasSize(2);
        assertThat(top2.get(0).getContent()).isEqualTo("많이 한 활동");
        assertThat(top2.get(0).getRecordCount()).isEqualTo(3L);
        assertThat(top2.get(1).getContent()).isEqualTo("중간 활동");
        assertThat(top2.get(1).getRecordCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("활동 ID 목록에 속한 기록들의 시간(시) 목록을 반환한다")
    void 기록의_시간대_목록을_반환한다() {
        User user = userRepository.save(User.builder()
                .socialId("social_b").nickname("유저B").role(Role.USER)
                .socialType(SocialType.KAKAO).deleted(false).build());
        Hobby hobby = hobbyRepository.save(Hobby.builder()
                .user(user).hobbyName("러닝").hobbyPurpose("건강")
                .hobbyTimeMinutes(20).executionCount(5)
                .status(HobbyStatus.IN_PROGRESS).build());
        Activity activity = createActivity(user, hobby, "아침 러닝");
        saveRecords(user, hobby, activity, 3);

        List<Integer> hours = activityRecordRepository.findRecordHoursByActivityIds(List.of(activity.getId()));

        assertThat(hours).hasSize(3);
        assertThat(hours).allSatisfy(hour -> assertThat(hour).isBetween(0, 23));
    }

    @Test
    @DisplayName("활동 ID 목록이 비어 있으면 빈 리스트를 반환한다")
    void 활동_ID가_없으면_빈_리스트를_반환한다() {
        assertThat(activityRecordRepository.findRecordHoursByActivityIds(List.of())).isEmpty();
    }

    private Activity createActivity(User user, Hobby hobby, String content) {
        return activityRepository.save(Activity.builder()
                .user(user).hobby(hobby).content(content).aiRecommended(false).build());
    }

    private void saveRecords(User user, Hobby hobby, Activity activity, int count) {
        for (int i = 0; i < count; i++) {
            activityRecordRepository.save(ActivityRecord.builder()
                    .activity(activity).hobby(hobby).user(user)
                    .visibility(RecordVisibility.PRIVATE)
                    .build());
        }
    }
}
