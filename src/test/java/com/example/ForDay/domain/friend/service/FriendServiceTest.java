package com.example.ForDay.domain.friend.service;

import com.example.ForDay.domain.friend.dto.request.AddFriendReqDto;
import com.example.ForDay.domain.friend.dto.request.BlockFriendReqDto;
import com.example.ForDay.domain.friend.dto.response.GetFriendListResDto;
import com.example.ForDay.domain.friend.entity.FriendRelation;
import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class FriendServiceTest {
    @Autowired
    UserRepository userRepository;

    @Autowired
    FriendService friendService;

    @Autowired
    FriendRelationRepository friendRelationRepository;

    @Test
    void 친구맺기_탈퇴한_유저인경우() {
        // given
        // 1. 요청자(A) 생성 및 저장
        User userA = User.builder()
                .nickname("요청자A")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_a")
                .deleted(false)
                .build();
        userRepository.save(userA);

        // 2. 탈퇴한 피요청자(B) 생성 및 저장
        User userB = User.builder()
                .nickname("탈퇴유저B")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_b")
                .deleted(true) // 탈퇴 처리
                .build();
        userRepository.save(userB);

        // 3. CustomUserDetails 설정 (시큐리티 컨텍스트 모킹)
        CustomUserDetails userDetails = new CustomUserDetails(userA);
        AddFriendReqDto reqDto = new AddFriendReqDto(userB.getId());

        // then (예외 나옴)
        // Service에서 CustomException(USER_NOT_FOUND)을 던지는지 검증
        CustomException exception = assertThrows(CustomException.class, () -> {
            friendService.addFriend(reqDto, userDetails);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 친구맺기_타겟유저가_요청자를_차단한경우() {
        // given
        // 1. 요청자(A) 생성 및 저장
        User userA = User.builder()
                .nickname("요청자A")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_a")
                .build();
        userRepository.save(userA);

        // 2. 탈퇴한 피요청자(B) 생성 및 저장
        User userB = User.builder()
                .nickname("요청자B")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_b")
                .build();
        userRepository.save(userB);
        // B -> A 차단
        FriendRelation relation = FriendRelation.builder()
                .requester(userB)
                .targetUser(userA)
                .relationStatus(FriendRelationStatus.BLOCK)
                .build();
        friendRelationRepository.save(relation);

        // when (A -> B에 친구 맺기 요청)
        CustomUserDetails userDetails = new CustomUserDetails(userA);
        AddFriendReqDto reqDto = new AddFriendReqDto(userB.getId());

        // then (예외 나옴)
        // Service에서 CustomException(USER_NOT_FOUND)을 던지는지 검증
        CustomException exception = assertThrows(CustomException.class, () -> {
            friendService.addFriend(reqDto, userDetails);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 친구맺기_요청자가_타겟유저를_차단한경우() {
        // given
        // 1. 요청자(A) 생성 및 저장
        User userA = User.builder()
                .nickname("요청자A")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_a")
                .build();
        userRepository.save(userA);

        // 2. 탈퇴한 피요청자(B) 생성 및 저장
        User userB = User.builder()
                .nickname("요청자B")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_b")
                .build();
        userRepository.save(userB);
        // A -> B 차단
        FriendRelation relation = FriendRelation.builder()
                .requester(userB)
                .targetUser(userA)
                .relationStatus(FriendRelationStatus.BLOCK)
                .build();
        friendRelationRepository.save(relation);

        // when (A -> B에 친구 맺기 요청)
        CustomUserDetails userDetails = new CustomUserDetails(userA);
        AddFriendReqDto reqDto = new AddFriendReqDto(userB.getId());

        // then (예외 나옴)
        // Service에서 CustomException(USER_NOT_FOUND)을 던지는지 검증
        CustomException exception = assertThrows(CustomException.class, () -> {
            friendService.addFriend(reqDto, userDetails);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 친구끊기_타겟유저가_탈퇴한_회원인경우() {
        // given
        // 1. 요청자(A) 생성 및 저장
        User userA = User.builder()
                .nickname("요청자A")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_a")
                .deleted(false)
                .build();
        userRepository.save(userA);

        // 2. 탈퇴한 피요청자(B) 생성 및 저장
        User userB = User.builder()
                .nickname("요청자B")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_b")
                .deleted(true)
                .build();
        userRepository.save(userB);

        // when
        CustomUserDetails userDetails = new CustomUserDetails(userA);

        // then (예외 나옴)
        // Service에서 CustomException(USER_NOT_FOUND)을 던지는지 검증
        CustomException exception = assertThrows(CustomException.class, () -> {
            friendService.deleteFriend(userB.getId(), userDetails);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 친구끊기_요청자가_타겟유저를_차단중인경우() {
        // given
        // 1. 요청자(A) 생성 및 저장
        User userA = User.builder()
                .nickname("요청자A")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_a")
                .build();
        userRepository.save(userA);

        // 2. 탈퇴한 피요청자(B) 생성 및 저장
        User userB = User.builder()
                .nickname("요청자B")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_b")
                .build();
        userRepository.save(userB);
        // A -> B 차단
        FriendRelation relation = FriendRelation.builder()
                .requester(userA)
                .targetUser(userB)
                .relationStatus(FriendRelationStatus.BLOCK)
                .build();
        friendRelationRepository.save(relation);

        // when
        CustomUserDetails userDetails = new CustomUserDetails(userA);

        // then (예외 나옴)
        // Service에서 CustomException(USER_NOT_FOUND)을 던지는지 검증
        CustomException exception = assertThrows(CustomException.class, () -> {
            friendService.deleteFriend(userB.getId(), userDetails);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 친구끊기_타겟유저가_요청자를_차단중인경우() {
        // given
        // 1. 요청자(A) 생성 및 저장
        User userA = User.builder()
                .nickname("요청자A")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_a")
                .build();
        userRepository.save(userA);

        // 2. 탈퇴한 피요청자(B) 생성 및 저장
        User userB = User.builder()
                .nickname("요청자B")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_b")
                .build();
        userRepository.save(userB);
        // B -> A 차단
        FriendRelation relation = FriendRelation.builder()
                .requester(userB)
                .targetUser(userA)
                .relationStatus(FriendRelationStatus.BLOCK)
                .build();
        friendRelationRepository.save(relation);

        // when (A -> B에 친구 맺기 요청)
        // then (예외 나옴)
        CustomUserDetails userDetails = new CustomUserDetails(userA);
        AddFriendReqDto reqDto = new AddFriendReqDto(userB.getId());

        // then (예외 나옴)
        // Service에서 CustomException(USER_NOT_FOUND)을 던지는지 검증
        CustomException exception = assertThrows(CustomException.class, () -> {
            friendService.addFriend(reqDto, userDetails);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 친구차단_타겟유저가_탈퇴한_회원인경우() {
        // given
        // 1. 요청자(A) 생성 및 저장
        User userA = User.builder()
                .nickname("요청자A")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_a")
                .deleted(false)
                .build();
        userRepository.save(userA);

        // 2. 탈퇴한 피요청자(B) 생성 및 저장
        User userB = User.builder()
                .nickname("요청자B")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_b")
                .deleted(true)
                .build();
        userRepository.save(userB);

        // when
        CustomUserDetails userDetails = new CustomUserDetails(userA);
        BlockFriendReqDto reqDto = new BlockFriendReqDto(userB.getId());

        // then (예외 나옴)
        // Service에서 CustomException(USER_NOT_FOUND)을 던지는지 검증
        CustomException exception = assertThrows(CustomException.class, () -> {
            friendService.blockFriend(reqDto, userDetails);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 친구차단_타겟유저가_요청유저를_차단한경우() {
        // given
        // 1. 요청자(A) 생성 및 저장
        User userA = User.builder()
                .nickname("요청자A")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_a")
                .build();
        userRepository.save(userA);

        // 2. 탈퇴한 피요청자(B) 생성 및 저장
        User userB = User.builder()
                .nickname("요청자B")
                .role(Role.USER)
                .socialType(SocialType.KAKAO)
                .socialId("social_b")
                .build();
        userRepository.save(userB);

        // B -> A 차단
        FriendRelation relation = FriendRelation.builder()
                .requester(userB)
                .targetUser(userA)
                .relationStatus(FriendRelationStatus.BLOCK)
                .build();
        friendRelationRepository.save(relation);

        // when
        CustomUserDetails userDetails = new CustomUserDetails(userA);
        BlockFriendReqDto reqDto = new BlockFriendReqDto(userB.getId());

        // then (예외 나옴)
        // Service에서 CustomException(USER_NOT_FOUND)을 던지는지 검증
        CustomException exception = assertThrows(CustomException.class, () -> {
            friendService.blockFriend(reqDto, userDetails);
        });

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void 친구목록조회_나를_차단한_사람은_친구목록에서_제외() {
        // given
        // 유저 생성 및 저장 (A, B, C, D)
        User userA = userRepository.save(User.builder().nickname("A").role(Role.USER).socialType(SocialType.KAKAO).socialId("A").build());
        User userB = userRepository.save(User.builder().nickname("B").role(Role.USER).socialType(SocialType.KAKAO).socialId("B").build());
        User userC = userRepository.save(User.builder().nickname("C").role(Role.USER).socialType(SocialType.KAKAO).socialId("C").build());
        User userD = userRepository.save(User.builder().nickname("D").role(Role.USER).socialType(SocialType.KAKAO).socialId("D").build());

        // A가 B, C, D를 순서대로 친구 추가 (A -> B, A -> C, A -> D)
        addFriend(userA, userB);
        addFriend(userA, userC);
        addFriend(userA, userD);

        // B가 A를 차단 (B -> A BLOCK)
        friendRelationRepository.save(FriendRelation.builder()
                .requester(userB)
                .targetUser(userA)
                .relationStatus(FriendRelationStatus.BLOCK)
                .build());

        // when
        CustomUserDetails userDetails = new CustomUserDetails(userA);
        GetFriendListResDto response = friendService.getFriendList(userDetails, null, 10);

        // then
        // 1. 전체 사이즈 검증 (B가 빠졌으므로 2명)
        assertEquals(2, response.getUserInfo().size());

        // 2. 정렬 순서 검증 (최신순: D -> C)
        // Querydsl 로직에서 createdAt.desc() 이므로 가장 마지막에 추가된 D가 첫 번째여야 함
        assertEquals(userD.getNickname(), response.getUserInfo().get(0).getNickname());
        assertEquals(userC.getNickname(), response.getUserInfo().get(1).getNickname());

        // 3. 차단한 유저(B)가 목록에 없는지 명시적 확인
        boolean containsB = response.getUserInfo().stream()
                .anyMatch(dto -> dto.getNickname().equals("B"));
        assertFalse(containsB);
    }

    private void addFriend(User requester, User targetUser) {
        FriendRelation relation = FriendRelation.builder()
                .requester(requester)
                .targetUser(targetUser)
                .relationStatus(FriendRelationStatus.FOLLOW)
                .build();
        friendRelationRepository.save(relation);
    }
}