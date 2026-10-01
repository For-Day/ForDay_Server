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
import com.example.ForDay.global.firebase.entity.FcmToken;
import com.example.ForDay.global.firebase.repository.FcmTokenRepository;
import com.example.ForDay.global.firebase.type.DeviceType;
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
    private static final String MEASURE_FCM_TOKEN_FORMAT = "measure-fcm-token-%s";

    @Value("${measure.seed.user-count:200}")
    private int userCount;

    @Value("${measure.seed.record-count:1000}")
    private int recordCount;

    private final UserRepository userRepository;
    private final HobbyRepository hobbyRepository;
    private final ActivityRepository activityRepository;
    private final ActivityRecordRepository recordRepository;
    private final FcmTokenRepository fcmTokenRepository;

    @Override
    public void run(String... args) {
        if (userRepository.existsBySocialId(SOCIAL_ID_FORMAT.formatted(1))) {
            log.info("[ReactionMeasurementSeeder] 이미 시드됨 - 기록 시드는 건너뛰고 푸시 수신 조건만 맞춘다");
            // 이 경로로도 반드시 들러야 한다 - #375 반응 측정 때 시드된 유저들은 푸시 수신
            // 조건이 없는 상태로 남아 있어서, 알림 측정을 그대로 돌리면 전부 조기 종료된다.
            ensurePushReceivable();
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
        ensurePushReceivable();

        log.info("[ReactionMeasurementSeeder] 유저 {}명, 기록 {}건 시드 완료", userCount, recordCount);
    }

    /**
     * 알림 파이프라인 측정의 전제 조건을 맞춘다.
     *
     * <p>{@code NotificationService#findActiveRecordDeviceToken}은 (1) 수신자의
     * {@code isRecordPushEnabled}가 켜져 있고 (2) FCM 토큰이 최소 1개 있을 때만 토큰 목록을
     * 돌려준다. 둘 중 하나라도 없으면 발송 경로가 통째로 건너뛰어져서, 부하를 아무리 걸어도
     * 푸시가 0건이 된다(= 측정 대상이 실행되지 않는다). 기본값은 둘 다 꺼짐이라 여기서 켜준다.
     */
    /**
     * {@code @Transactional}을 붙이지 않는다. 이 메서드는 {@link #run}이 같은 빈 안에서
     * 호출하므로 Spring AOP 프록시를 거치지 않아 트랜잭션이 어차피 적용되지 않는다. 처음에는
     * 붙여뒀다가, 더티 체킹이 플러시되지 않아 132건의 푸시 활성화가 통째로 사라지는 걸
     * 실측으로 발견했다. 변경분은 명시적으로 저장한다.
     */
    public void ensurePushReceivable() {
        List<User> changed = new ArrayList<>();
        int tokensCreated = 0;

        // measure_user_N만 손보면 부족하다. k6는 기록 ID로 대상을 고르는데, 그 범위에는
        // 로컬 개발 중 쌓인 다른 유저의 기록도 섞여 있다. 1차 측정에서 대상 기록 997건 중
        // 132건이 푸시 비활성 유저 소유라 발송이 일어나지 않았고, 그만큼이 "유실"로
        // 잘못 잡혔다. 기록을 가진 유저는 전부 수신 가능 상태로 맞춘다.
        for (User user : userRepository.findAll()) {
            if (!user.isRecordPushEnabled()) {
                user.updateRecordPushEnabled(true);
                changed.add(user);
            }

            // 토큰 문자열에 유저 ID를 넣어 유니크 제약을 만족시킨다. 실제 FCM으로 나가지
            // 않는 값이다 - measure 프로파일에서는 MeasuringPushSenderAdapter가 받는다.
            // 유저당 정확히 하나만 만든다 - "알림 1건 = 발송 1건"이 성립해야 중복 집계가 맞다.
            if (fcmTokenRepository.findByUserId(user.getId()).isEmpty()) {
                fcmTokenRepository.save(FcmToken.createFcmToken(
                        "measure-device", user, MEASURE_FCM_TOKEN_FORMAT.formatted(user.getId()), DeviceType.ANDROID));
                tokensCreated++;
            }
        }

        userRepository.saveAll(changed);

        log.info("[ReactionMeasurementSeeder] 푸시 수신 조건 정리 - 알림 활성화 {}명, FCM 토큰 생성 {}개",
                changed.size(), tokensCreated);
    }

    private List<User> createUsers() {
        List<User> users = new ArrayList<>(userCount);
        for (int i = 1; i <= userCount; i++) {
            users.add(User.builder()
                    .nickname("measure" + i)
                    .socialId(SOCIAL_ID_FORMAT.formatted(i))
                    .role(Role.GUEST)
                    .socialType(SocialType.GUEST)
                    .isRecordPushEnabled(true)
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
