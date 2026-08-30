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
import com.example.ForDay.support.IntegrationTestSupport;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
class FriendServiceTest extends IntegrationTestSupport {
    @Autowired
    UserRepository userRepository;

    @Autowired
    FriendService friendService;

    @Autowired
    FriendRelationRepository friendRelationRepository;

    @Autowired
    EntityManager entityManager;

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
        // 차단 상태면 FOLLOW 관계가 없으므로 끊을 친구가 없다
        CustomException exception = assertThrows(CustomException.class, () -> {
            friendService.deleteFriend(userB.getId(), userDetails);
        });

        assertEquals(ErrorCode.FRIEND_NOT_FOUND, exception.getErrorCode());
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

        // when (A -> B에 친구 끊기 요청)
        CustomUserDetails userDetails = new CustomUserDetails(userA);

        // then (예외 나옴)
        // 차단당한 방향이라 A->B FOLLOW 관계가 없다
        CustomException exception = assertThrows(CustomException.class, () -> {
            friendService.deleteFriend(userB.getId(), userDetails);
        });

        assertEquals(ErrorCode.FRIEND_NOT_FOUND, exception.getErrorCode());
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
        // createdAt은 @PrePersist가 넣는 값이라 세 건이 같은 밀리초에 저장되면 동점이 된다.
        // 그러면 정렬이 타이브레이커인 랜덤 UUID로 결정돼 순서 단언이 실행마다 뒤집히므로,
        // 추가된 시각을 명시적으로 벌려 최신순을 결정적으로 만든다.
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0);
        addFriend(userA, userB, base);
        addFriend(userA, userC, base.plusMinutes(1));
        addFriend(userA, userD, base.plusMinutes(2));

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

    /**
     * createdAt을 지정해 친구 관계를 만든다.
     *
     * <p>createdAt은 {@code @PrePersist}가 채우고 세터가 없으므로, 저장 후 네이티브 쿼리로
     * 덮어쓴다. 정렬 순서를 검증하는 테스트가 시계 정밀도에 좌우되지 않게 하려는 목적이다.
     */
    private void addFriend(User requester, User targetUser, LocalDateTime createdAt) {
        FriendRelation relation = FriendRelation.builder()
                .requester(requester)
                .targetUser(targetUser)
                .relationStatus(FriendRelationStatus.FOLLOW)
                .build();
        friendRelationRepository.saveAndFlush(relation);

        entityManager.createNativeQuery(
                        "UPDATE friend_relations SET created_at = :createdAt WHERE friend_relation_id = :id")
                .setParameter("createdAt", createdAt)
                .setParameter("id", relation.getId())
                .executeUpdate();
        entityManager.clear();
    }
}
