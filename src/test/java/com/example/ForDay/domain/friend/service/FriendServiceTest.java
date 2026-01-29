package com.example.ForDay.domain.friend.service;

import com.example.ForDay.domain.friend.dto.request.AddFriendReqDto;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
        // B -> A 차단
        FriendRelation relation = FriendRelation.builder()
                .requester(userA)
                .targetUser(userB)
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

}