package com.example.ForDay.domain.user.service;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.friend.dto.request.AddFriendReqDto;
import com.example.ForDay.domain.friend.dto.response.GetFriendListResDto;
import com.example.ForDay.domain.friend.entity.FriendRelation;
import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.dto.response.GetUserFeedListResDto;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
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
class UserServiceTest {
    @Autowired
    UserRepository userRepository;

    @Autowired
    FriendRelationRepository friendRelationRepository;

    @Autowired
    HobbyRepository hobbyRepository;

    @Autowired
    ActivityRepository activityRepository;

    @Autowired
    ActivityRecordRepository activityRecordRepository;

    @Autowired
    UserService userService;

    @Test
    void 사용자_피드목록조회_자신의_피드_조회시() {
        // given
        User userA = User.builder()
                .nickname("피드 주인A")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_a")
                .build();
        userRepository.save(userA);

        ActivityRecord public1 = createRecord(userA, RecordVisibility.PUBLIC);
        ActivityRecord public2 = createRecord(userA, RecordVisibility.PUBLIC);
        ActivityRecord friend1 = createRecord(userA, RecordVisibility.FRIEND);
        ActivityRecord friend2 = createRecord(userA, RecordVisibility.FRIEND);
        ActivityRecord private1 = createRecord(userA, RecordVisibility.PRIVATE);
        ActivityRecord private2 = createRecord(userA, RecordVisibility.PRIVATE);

        // when (A -> B에 친구 맺기 요청)
        CustomUserDetails userDetails = new CustomUserDetails(userA);
        GetUserFeedListResDto response = userService.getUserFeedList(List.of(), null, 10, userDetails, userA.getId());

        List<GetUserFeedListResDto.FeedDto> feedList = response.getFeedList();

        // then
        assertEquals(6, feedList.size());

        assertEquals(private2.getId(), feedList.get(0).getRecordId());
        assertEquals(private1.getId(), feedList.get(1).getRecordId());
        assertEquals(friend2.getId(), feedList.get(2).getRecordId());
        assertEquals(friend1.getId(), feedList.get(3).getRecordId());
        assertEquals(public2.getId(), feedList.get(4).getRecordId());
        assertEquals(public1.getId(), feedList.get(5).getRecordId());
    }

    @Test
    void 사용자_피드목록조회_친구관계인_유저가_조회시() {
        // given
        User userA = User.builder()
                .nickname("피드 주인A")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_a")
                .build();
        userRepository.save(userA);

        User userB = User.builder()
                .nickname("조회자B")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_b")
                .build();
        userRepository.save(userB);

        // B -> A 친구 추가
        FriendRelation relation = FriendRelation.builder()
                .requester(userB)
                .targetUser(userA)
                .relationStatus(FriendRelationStatus.FOLLOW)
                .build();
        friendRelationRepository.save(relation);

        ActivityRecord public1 = createRecord(userA, RecordVisibility.PUBLIC);
        ActivityRecord public2 = createRecord(userA, RecordVisibility.PUBLIC);
        ActivityRecord friend1 = createRecord(userA, RecordVisibility.FRIEND);
        ActivityRecord friend2 = createRecord(userA, RecordVisibility.FRIEND);
        ActivityRecord private1 = createRecord(userA, RecordVisibility.PRIVATE);
        ActivityRecord private2 = createRecord(userA, RecordVisibility.PRIVATE);

        // when
        CustomUserDetails userDetails = new CustomUserDetails(userB);
        GetUserFeedListResDto response = userService.getUserFeedList(List.of(), null, 10, userDetails, userA.getId());

        List<GetUserFeedListResDto.FeedDto> feedList = response.getFeedList();

        // then
        assertEquals(4, feedList.size());

        assertEquals(friend2.getId(), feedList.get(0).getRecordId());
        assertEquals(friend1.getId(), feedList.get(1).getRecordId());
        assertEquals(public2.getId(), feedList.get(2).getRecordId());
        assertEquals(public1.getId(), feedList.get(3).getRecordId());

        boolean containsPrivate1 = feedList.stream().anyMatch(f -> f.getRecordId().equals(private1.getId()));
        boolean containsPrivate2 = feedList.stream().anyMatch(f -> f.getRecordId().equals(private2.getId()));

        assertFalse(containsPrivate1, "PRIVATE 게시글(pr1)이 포함되었습니다.");
        assertFalse(containsPrivate2, "PRIVATE 게시글(pr2)이 포함되었습니다.");
    }

    @Test
    void 사용자_피드목록조회_친구가아닌_유저가_조회시() {
        // given
        User userA = User.builder()
                .nickname("피드 주인A")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_a")
                .build();
        userRepository.save(userA);

        User userB = User.builder()
                .nickname("조회자B")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_b")
                .build();
        userRepository.save(userB);

        ActivityRecord public1 = createRecord(userA, RecordVisibility.PUBLIC);
        ActivityRecord public2 = createRecord(userA, RecordVisibility.PUBLIC);
        ActivityRecord friend1 = createRecord(userA, RecordVisibility.FRIEND);
        ActivityRecord friend2 = createRecord(userA, RecordVisibility.FRIEND);
        ActivityRecord private1 = createRecord(userA, RecordVisibility.PRIVATE);
        ActivityRecord private2 = createRecord(userA, RecordVisibility.PRIVATE);

        // when
        CustomUserDetails userDetails = new CustomUserDetails(userB);
        GetUserFeedListResDto response = userService.getUserFeedList(List.of(), null, 10, userDetails, userA.getId());

        List<GetUserFeedListResDto.FeedDto> feedList = response.getFeedList();

        // then
        assertEquals(2, feedList.size());

        assertEquals(public2.getId(), feedList.get(0).getRecordId());
        assertEquals(public1.getId(), feedList.get(1).getRecordId());

        boolean containsPrivate1 = feedList.stream().anyMatch(f -> f.getRecordId().equals(private1.getId()));
        boolean containsPrivate2 = feedList.stream().anyMatch(f -> f.getRecordId().equals(private2.getId()));
        boolean containsFriend1 = feedList.stream().anyMatch(f -> f.getRecordId().equals(friend1.getId()));
        boolean containsFriend2 = feedList.stream().anyMatch(f -> f.getRecordId().equals(friend2.getId()));


        assertFalse(containsPrivate1, "PRIVATE 게시글이 포함되었습니다.");
        assertFalse(containsPrivate2, "PRIVATE 게시글이 포함되었습니다.");

        assertFalse(containsFriend1, "FRIEND 게시글이 포함되었습니다.");
        assertFalse(containsFriend2, "FRIEND 게시글이 포함되었습니다.");
    }

    @Test
    void 사용자_피드목록조회_차단당한_유저가_조회시() {
        // given
        User userA = User.builder()
                .nickname("피드 주인A")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_a")
                .build();
        userRepository.save(userA);

        User userB = User.builder()
                .nickname("차단당한 유저B")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_b")
                .build();
        userRepository.save(userB);

        FriendRelation relation = FriendRelation.builder()
                .requester(userA)
                .targetUser(userB)
                .relationStatus(FriendRelationStatus.BLOCK)
                .build();
        friendRelationRepository.save(relation);

        ActivityRecord public1 = createRecord(userA, RecordVisibility.PUBLIC);
        ActivityRecord public2 = createRecord(userA, RecordVisibility.PUBLIC);
        ActivityRecord friend1 = createRecord(userA, RecordVisibility.FRIEND);
        ActivityRecord friend2 = createRecord(userA, RecordVisibility.FRIEND);
        ActivityRecord private1 = createRecord(userA, RecordVisibility.PRIVATE);
        ActivityRecord private2 = createRecord(userA, RecordVisibility.PRIVATE);

        // when
        CustomUserDetails userDetails = new CustomUserDetails(userB);

        // then
        // Service에서 CustomException(USER_NOT_FOUND)을 던지는지 검증
        CustomException exception = assertThrows(CustomException.class, () -> {
            userService.getUserFeedList(List.of(), null, 10, userDetails, userA.getId());
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    private ActivityRecord createRecord(User userA, RecordVisibility visibility) {
        Hobby hobby = Hobby.builder()
                .user(userA)
                .hobbyName("취미명1")
                .hobbyPurpose("취미목적1")
                .hobbyTimeMinutes(30)
                .executionCount(4)
                .status(HobbyStatus.IN_PROGRESS)
                .build();
        hobbyRepository.save(hobby);

        Activity activity = Activity
                .builder()
                .user(userA)
                .hobby(hobby)
                .content("내용")
                .build();
        activityRepository.save(activity);

        ActivityRecord activityRecord = ActivityRecord
                .builder()
                .activity(activity)
                .hobby(hobby)
                .user(userA)
                .sticker("스티커")
                .visibility(visibility)
                .build();
        activityRecordRepository.save(activityRecord);
        return activityRecord;
    }
}