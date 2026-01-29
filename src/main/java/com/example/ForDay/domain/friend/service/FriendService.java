package com.example.ForDay.domain.friend.service;

import com.example.ForDay.domain.friend.dto.request.AddFriendReqDto;
import com.example.ForDay.domain.friend.dto.request.BlockFriendReqDto;
import com.example.ForDay.domain.friend.dto.response.AddFriendResDto;
import com.example.ForDay.domain.friend.dto.response.BlockFriendResDto;
import com.example.ForDay.domain.friend.dto.response.DeleteFriendResDto;
import com.example.ForDay.domain.friend.entity.FriendRelation;
import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FriendService {
    private final FriendRelationRepository friendRelationRepository;
    private final UserUtil userUtil;
    private final UserRepository userRepository;

    @Transactional
    public AddFriendResDto addFriend(AddFriendReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        User targetUser = userRepository.findById(reqDto.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String currentUserId = currentUser.getId();
        String targetUserId = targetUser.getId();

        // 자기 자신 체크
        if (currentUserId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.CANNOT_FOLLOW_SELF);
        }

        // 상대방이 나를 차단했는지 확인
        Optional<FriendRelation> blockedByTarget = friendRelationRepository
                .findByRequesterIdAndTargetId(targetUserId, currentUserId);

        if (blockedByTarget.isPresent() && blockedByTarget.get().getRelationStatus() == FriendRelationStatus.BLOCK) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 내가 맺은 관계 확인
        Optional<FriendRelation> myRelation = friendRelationRepository
                .findByRequesterIdAndTargetId(currentUserId, targetUserId);

        if (myRelation.isPresent()) {
            FriendRelation relation = myRelation.get();
            if (relation.getRelationStatus() == FriendRelationStatus.FOLLOW) {
                return new AddFriendResDto("이미 친구 맺기가 되어있습니다.", targetUser.getNickname());
            } else if (relation.getRelationStatus() == FriendRelationStatus.BLOCK) {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }
        }

        // 새로운 친구 관계 생성
        friendRelationRepository.save(FriendRelation.builder()
                .requester(currentUser)
                .targetUser(targetUser)
                .relationStatus(FriendRelationStatus.FOLLOW)
                .build());

        return new AddFriendResDto("성공적으로 친구 맺기가 되었습니다.", targetUser.getNickname());
    }

    @Transactional
    public DeleteFriendResDto deleteFriend(String friendId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        User targetUser = userRepository.findById(friendId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String currentUserId = currentUser.getId();
        String targetUserId = targetUser.getId();

        // 내가 맺은 관계 조회 (나 -> 상대)
        FriendRelation myRelation = friendRelationRepository
                .findByRequesterIdAndTargetId(currentUserId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_NOT_FOUND));

        // 현재 상태가 FOLLOW일 때만 삭제 가능. 만약 내가 상대를 BLOCK 중이라면 삭제(언팔로우) 대상이 아님.
        if (myRelation.getRelationStatus() != FriendRelationStatus.FOLLOW) {
            throw new CustomException(ErrorCode.FRIEND_NOT_FOUND);
        }

        // 상대방이 나를 차단했는지 확인
        // 친구 추가와 동일하게 상대가 나를 차단했다면 정보 노출 방지를 위해 '찾을 수 없음' 처리
        Optional<FriendRelation> blockedByTarget = friendRelationRepository
                .findByRequesterIdAndTargetId(targetUserId, currentUserId);

        if (blockedByTarget.isPresent() && blockedByTarget.get().getRelationStatus() == FriendRelationStatus.BLOCK) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        friendRelationRepository.delete(myRelation);

        return new DeleteFriendResDto("성공적으로 친구 관계를 삭제했습니다.", targetUser.getNickname());
    }

    @Transactional
    public BlockFriendResDto blockFriend(BlockFriendReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        User targetUser = userRepository.findById(reqDto.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String currentUserId = currentUser.getId();
        String targetUserId = targetUser.getId();

        if (currentUserId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.CANNOT_BLOCK_SELF);
        }

        // 상대방이 나를 차단했는지 확인
        Optional<FriendRelation> blockedByTarget = friendRelationRepository
                .findByRequesterIdAndTargetId(targetUserId, currentUserId);

        if (blockedByTarget.isPresent() && blockedByTarget.get().getRelationStatus() == FriendRelationStatus.BLOCK) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 나의 차단 로직 수행
        Optional<FriendRelation> myRelation = friendRelationRepository
                .findByRequesterIdAndTargetId(currentUserId, targetUserId);

        if (myRelation.isPresent()) {
            FriendRelation relation = myRelation.get();

            if (relation.getRelationStatus() == FriendRelationStatus.BLOCK) {
                return new BlockFriendResDto("이미 차단된 상태입니다.", targetUser.getNickname());
            }

            // FOLLOW 상태에서 BLOCK으로 변경
            relation.changeStatus(FriendRelationStatus.BLOCK);
        } else {
            friendRelationRepository.save(FriendRelation.builder()
                    .requester(currentUser)
                    .targetUser(targetUser)
                    .relationStatus(FriendRelationStatus.BLOCK)
                    .build());
        }

        return new BlockFriendResDto("성공적으로 차단되었습니다.", targetUser.getNickname());
    }
}
