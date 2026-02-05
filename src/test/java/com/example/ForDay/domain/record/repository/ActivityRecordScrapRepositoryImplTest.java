package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.friend.entity.FriendRelation;
import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.ActivityRecordScrap;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.dto.response.GetUserScrapListResDto;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class ActivityRecordScrapRepositoryImplTest {
    @Autowired
    private ActivityRecordRepository activityRecordRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FriendRelationRepository friendRelationRepository;

    @Autowired
    private ActivityRecordScrapRepository activityRecordScrapRepository;

    @Autowired
    private HobbyRepository hobbyRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Test
    void 다른사용자의_스크랩목록_복합필터링_검증() {
        // 1. 유저 생성 (G는 탈퇴 상태)
        User userA = createAndSaveUser("social_a", "유저A", false);
        User userB = createAndSaveUser("social_b", "유저B", false);
        User userC = createAndSaveUser("social_c", "유저C", false);
        User userD = createAndSaveUser("social_d", "유저D", false);
        User userE = createAndSaveUser("social_e", "유저E", false);
        User userF = createAndSaveUser("social_f", "유저F", false);
        User userG = createAndSaveUser("social_g", "유저G", true); // 탈퇴 유저

        // 2. 친구 및 차단 관계 설정
        saveRelation(userB, userA, FriendRelationStatus.FOLLOW); // B <-> A 친구
        saveRelation(userB, userD, FriendRelationStatus.FOLLOW); // B <-> D 친구
        saveRelation(userA, userD, FriendRelationStatus.FOLLOW); // A <-> D 친구
        saveRelation(userF, userA, FriendRelationStatus.BLOCK);  // F <-> A 차단

        // 3. 기록 생성 (각 유저별)
        ActivityRecord rec1 = createRecord(userA, RecordVisibility.FRIEND); // 기록1
        ActivityRecord rec2 = createRecord(userB, RecordVisibility.PRIVATE); // 기록2
        ActivityRecord rec3 = createRecord(userC, RecordVisibility.PUBLIC); // 기록3
        ActivityRecord rec4 = createRecord(userD, RecordVisibility.FRIEND); // 기록4
        ActivityRecord rec5 = createRecord(userE, RecordVisibility.FRIEND); // 기록5
        ActivityRecord rec6 = createRecord(userF, RecordVisibility.PUBLIC); // 기록6
        ActivityRecord rec7 = createRecord(userG, RecordVisibility.PUBLIC); // 기록7

        // 4. B가 위 모든 기록을 스크랩함
        saveScrap(userB, rec1);
        saveScrap(userB, rec2);
        saveScrap(userB, rec3);
        saveScrap(userB, rec4);
        saveScrap(userB, rec5);
        saveScrap(userB, rec6);
        saveScrap(userB, rec7);

        // 5. 유저 A가 유저 B의 스크랩 목록을 조회 (targetUserId = userB.getId())
        List<String> myFriendIds = friendRelationRepository.findAllFriendIdsByUserId(userA.getId());
        List<String> blockFriendIds = friendRelationRepository.findAllBlockedIdsByUserId(userA.getId());

        // getOtherScrapList 호출
        List<GetUserScrapListResDto.ScrapDto> result = activityRecordScrapRepository.getOtherScrapList(
                null, 10, userB.getId(), userA.getId(), myFriendIds, blockFriendIds,
                List.of());

        // 6. 결과 검증 (기록1, 기록3, 기록4만 조회되어야 함)
        assertEquals(3, result.size());

        // 상세 ID 검증 (순서는 쿼리 정렬 기준에 따라 다를 수 있음)
        List<Long> resultRecordIds = result.stream()
                .map(GetUserScrapListResDto.ScrapDto::getRecordId)
                .toList();

        assertTrue(resultRecordIds.contains(rec1.getId())); // 본인 기록 (FRIEND)
        assertTrue(resultRecordIds.contains(rec3.getId())); // 타인 기록 (PUBLIC)
        assertTrue(resultRecordIds.contains(rec4.getId())); // 친구 기록 (FRIEND)

        assertFalse(resultRecordIds.contains(rec2.getId())); // PRIVATE 제외
        assertFalse(resultRecordIds.contains(rec5.getId())); // 남의 친구글 제외
        assertFalse(resultRecordIds.contains(rec6.getId())); // 차단 제외
        assertFalse(resultRecordIds.contains(rec7.getId())); // 탈퇴 제외
    }

    private User createAndSaveUser(String socialId, String nickname, boolean deleted) {
        return userRepository.save(User.builder()
                .socialId(socialId).nickname(nickname).role(Role.USER)
                .socialType(SocialType.KAKAO).deleted(deleted).build());
    }

    private void saveRelation(User req, User target, FriendRelationStatus status) {
        friendRelationRepository.save(FriendRelation.builder()
                .requester(req).targetUser(target).relationStatus(status).build());
    }

    private ActivityRecord createRecord(User user, RecordVisibility visibility) {
        // Activity 생성 로직 포함하여 저장 후 반환
        Hobby hobby = Hobby.builder()
                .user(user)
                .hobbyName("취미" + user.getNickname())
                .hobbyPurpose("목적")
                .hobbyTimeMinutes(10)
                .executionCount(4)
                .status(HobbyStatus.IN_PROGRESS)
                .build();
        hobbyRepository.save(hobby);

        Activity activity = Activity.builder()
                .user(user)
                .hobby(hobby)
                .content("내용")
                .aiRecommended(true)
                .build();
        activityRepository.save(activity);

        ActivityRecord activityRecord = ActivityRecord.builder()
                .activity(activity)
                .hobby(hobby)
                .user(user)
                .sticker("스티커")
                .visibility(visibility)
                .build();
        return activityRecordRepository.save(activityRecord);
    }

    private void saveScrap(User user, ActivityRecord record) {
        activityRecordScrapRepository.save(ActivityRecordScrap.builder()
                .user(user).activityRecord(record).build());
    }

}