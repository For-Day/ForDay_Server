package com.example.ForDay.domain.reaction.measure;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * #374 부하 테스트 측정 환경 전용 데이터 시드. {@code ReactionInitializer}(local 프로파일)는
 * 유저 20명·기록 1개짜리 더미라 1,000 VU 규모 부하테스트에는 못 쓴다 — 이 클래스는 같은
 * 셋업 패턴이되 규모를 프로퍼티로 조절 가능하게 만든 버전이다.
 *
 * <p>유저의 {@code socialId}를 {@code measure_user_{n}} 같은 예측 가능한 패턴으로 만드는
 * 이유 — k6 {@code setup()}이 매 실행마다 새 유저를 만드는 대신, 이미 시드된 유저의
 * {@code guestUserId}로 {@code /auth/guest}를 호출해 로그인하게 하기 위함이다. 이렇게 하면
 * #375의 4단계(①~④)를 전부 같은 유저·기록 풀로 재측정할 수 있어 DB를 매번 초기화할 필요가
 * 없다.
 *
 * <p>이미 시드돼 있으면(첫 번째 유저의 socialId가 존재하면) 재실행하지 않는다 — 앱을
 * 재시작할 때마다(4단계 전환 중 재배포가 필요할 수 있음) 중복 시드되는 것을 막기 위함이다.
 */
@Slf4j
@Component
@Profile("measure")
@RequiredArgsConstructor
public class ReactionMeasurementSeeder implements CommandLineRunner {

    private static final String SOCIAL_ID_FORMAT = "measure_user_%d";

    @Value("${measure.seed.user-count:200}")
    private int userCount;

    @Value("${measure.seed.record-count:1000}")
    private int recordCount;

    private final UserRepository userRepository;
    private final HobbyRepository hobbyRepository;
    private final ActivityRepository activityRepository;
    private final ActivityRecordRepository recordRepository;

    @Override
    public void run(String... args) {
        if (userRepository.existsBySocialId(SOCIAL_ID_FORMAT.formatted(1))) {
            log.info("[ReactionMeasurementSeeder] 이미 시드됨 - 건너뜀");
            return;
        }
        seed();
    }

    @Transactional
    public void seed() {
        List<User> users = createUsers();
        List<Hobby> hobbies = createHobbies(users);
        List<Activity> activities = createActivities(users, hobbies);
        createRecords(users, hobbies, activities);

        log.info("[ReactionMeasurementSeeder] 유저 {}명, 기록 {}건 시드 완료", userCount, recordCount);
    }

    private List<User> createUsers() {
        List<User> users = new ArrayList<>(userCount);
        for (int i = 1; i <= userCount; i++) {
            users.add(User.builder()
                    .nickname("measure" + i)
                    .socialId(SOCIAL_ID_FORMAT.formatted(i))
                    .role(Role.GUEST)
                    .socialType(SocialType.GUEST)
                    .build());
        }
        return userRepository.saveAll(users);
    }

    private List<Hobby> createHobbies(List<User> users) {
        List<Hobby> hobbies = new ArrayList<>(users.size());
        for (User user : users) {
            hobbies.add(Hobby.builder()
                    .user(user)
                    .hobbyInfoId(1L)
                    .hobbyPurpose("부하테스트 목적")
                    .hobbyTimeMinutes(1)
                    .executionCount(4)
                    .status(HobbyStatus.IN_PROGRESS)
                    .hobbyName("부하테스트 취미")
                    .build());
        }
        return hobbyRepository.saveAll(hobbies);
    }

    private List<Activity> createActivities(List<User> users, List<Hobby> hobbies) {
        List<Activity> activities = new ArrayList<>(users.size());
        for (int i = 0; i < users.size(); i++) {
            activities.add(Activity.builder()
                    .user(users.get(i))
                    .hobby(hobbies.get(i))
                    .content("부하테스트용 활동 기록")
                    .aiRecommended(false)
                    .build());
        }
        return activityRepository.saveAll(activities);
    }

    private void createRecords(List<User> users, List<Hobby> hobbies, List<Activity> activities) {
        List<ActivityRecord> records = new ArrayList<>(recordCount);
        for (int i = 0; i < recordCount; i++) {
            int writerIndex = i % users.size();
            records.add(ActivityRecord.builder()
                    .user(users.get(writerIndex))
                    .memo("부하테스트용 기록 #" + i)
                    .sticker("🔥")
                    .activity(activities.get(writerIndex))
                    .hobby(hobbies.get(writerIndex))
                    .visibility(RecordVisibility.PUBLIC)
                    .build());
        }
        recordRepository.saveAll(records);
    }
}
