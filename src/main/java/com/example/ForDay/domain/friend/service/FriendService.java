package com.example.ForDay.domain.friend.service;

import com.example.ForDay.domain.friend.dto.request.AddFriendReqDto;
import com.example.ForDay.domain.friend.dto.response.AddFriendResDto;
import com.example.ForDay.domain.friend.entity.FriendRelation;
import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
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
}
