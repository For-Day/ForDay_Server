package com.example.ForDay.domain.friend.service;

import com.example.ForDay.domain.friend.dto.request.AddFriendReqDto;
import com.example.ForDay.domain.friend.dto.request.BlockFriendReqDto;
import com.example.ForDay.domain.friend.dto.request.ReportFriendReqDto;
import com.example.ForDay.domain.friend.dto.response.*;
import com.example.ForDay.domain.friend.entity.FriendRelation;
import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.util.S3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.example.ForDay.global.common.response.message.FriendSuccessMessage.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {
    private final FriendRelationRepository friendRelationRepository;
    private final UserUtil userUtil;
    private final UserRepository userRepository;
    private final S3Util s3Util;
    private final FriendCacheService friendCacheService;

    @Transactional
    public AddFriendResDto addFriend(AddFriendReqDto reqDto, CustomUserDetails userDetails) {
        User currentUser = userUtil.getCurrentUser(userDetails);
        User targetUser = findTargetUser(reqDto.getUserId());

        if (validateBySelf(currentUser, targetUser)) {
            throw new CustomException(ErrorCode.CANNOT_FOLLOW_SELF);
        }

        List<FriendRelation> relations = friendRelationRepository.findBothDirections(currentUser.getId(), targetUser.getId());
        FriendRelation myRelation = findInRelations(relations, currentUser.getId());
        FriendRelation targetRelation = findInRelations(relations, targetUser.getId());

        validateNotBlockedByTarget(targetRelation);

        if (myRelation != null) {
            FriendRelationStatus status = myRelation.getRelationStatus();
            if (status == FriendRelationStatus.FOLLOW) {
                return AddFriendResDto.of(ALREADY_FRIEND, targetUser.getNickname());
            }
            if (status == FriendRelationStatus.BLOCK || status == FriendRelationStatus.REPORT) {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }
            myRelation.changeStatus(FriendRelationStatus.FOLLOW);
        } else {
            friendRelationRepository.save(FriendRelation.of(currentUser, targetUser, FriendRelationStatus.FOLLOW));
        }
        friendCacheService.evictFriendCache(currentUser.getId(), targetUser.getId());
        return AddFriendResDto.of(ADD_FRIEND_SUCCESS, targetUser.getNickname());
    }

    @Transactional
    public DeleteFriendResDto deleteFriend(String friendId, CustomUserDetails userDetails) {
        User currentUser = userUtil.getCurrentUser(userDetails);
        User targetUser = findTargetUser(friendId);

        FriendRelation myRelation = friendRelationRepository
                .findByRequesterIdAndTargetUserId(currentUser.getId(), targetUser.getId())
                .filter(r -> r.getRelationStatus() == FriendRelationStatus.FOLLOW)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_NOT_FOUND));

        friendRelationRepository.delete(myRelation);
        friendCacheService.evictFriendCache(currentUser.getId(), targetUser.getId());
        return DeleteFriendResDto.of(DELETE_FRIEND_SUCCESS, targetUser.getNickname());
    }

    @Transactional
    public BlockFriendResDto blockFriend(BlockFriendReqDto reqDto, CustomUserDetails userDetails) {
        User currentUser = userUtil.getCurrentUser(userDetails);
        User targetUser = findTargetUser(reqDto.getUserId());

        if (validateBySelf(currentUser, targetUser)) {
            throw new CustomException(ErrorCode.CANNOT_BLOCK_SELF);
        }

        List<FriendRelation> relations = friendRelationRepository.findBothDirections(currentUser.getId(), targetUser.getId());
        FriendRelation myRelation = findInRelations(relations, currentUser.getId());
        FriendRelation targetRelation = findInRelations(relations, targetUser.getId());

        validateNotBlockedByTarget(targetRelation);

        if (myRelation != null) {
            if (myRelation.getRelationStatus() == FriendRelationStatus.BLOCK) {
                return BlockFriendResDto.of(ALREADY_BLOCKED, targetUser.getNickname());
            }
            myRelation.changeStatus(FriendRelationStatus.BLOCK);
        } else {
            friendRelationRepository.save(FriendRelation.of(currentUser, targetUser, FriendRelationStatus.BLOCK));
        }
        friendCacheService.evictFriendCache(currentUser.getId(), targetUser.getId());
        return BlockFriendResDto.of(targetUser.getNickname() + "님이 차단되었어요.", targetUser.getNickname());
    }

    @Transactional(readOnly = true)
    public GetFriendListResDto getFriendList(CustomUserDetails user, String lastUserId, Integer size) {
        User currentUser = userUtil.getCurrentUser(user);
        List<GetFriendListResDto.UserInfoDto> userInfoDtos = friendRelationRepository.findMyFriendList(currentUser.getId(), lastUserId, size + 1);

        List<GetFriendListResDto.UserInfoDto> updatedList = GetFriendListResDto.UserInfoDto.listOf(
                userInfoDtos,
                s3Util::toProfileMainResizedUrl
        );

        return GetFriendListResDto.of(FRIEND_LIST_GET_SUCCESS, updatedList, size);
    }

    @Transactional
    public ReportFriendResDto reportFriend(ReportFriendReqDto reqDto, CustomUserDetails userDetails) {
        User currentUser = userUtil.getCurrentUser(userDetails);
        User targetUser = findTargetUser(reqDto.getUserId());

        if (validateBySelf(currentUser, targetUser)) {
            throw new CustomException(ErrorCode.CANNOT_REPORT_SELF);
        }

        List<FriendRelation> relations = friendRelationRepository.findBothDirections(currentUser.getId(), targetUser.getId());
        FriendRelation myRelation = findInRelations(relations, currentUser.getId());
        FriendRelation targetRelation = findInRelations(relations, targetUser.getId());

        validateNotBlockedByTarget(targetRelation);

        if (myRelation != null) {
            FriendRelationStatus status = myRelation.getRelationStatus();
            if (status == FriendRelationStatus.REPORT) {
                return ReportFriendResDto.of(ALREADY_REPORTED, targetUser);
            } else if (status == FriendRelationStatus.BLOCK) {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }
            myRelation.changeStatus(FriendRelationStatus.REPORT);
        } else {
            friendRelationRepository.save(FriendRelation.of(currentUser, targetUser, FriendRelationStatus.REPORT));
        }
        friendCacheService.evictFriendCache(currentUser.getId(), targetUser.getId());
        return ReportFriendResDto.of(REPORT_FRIEND_SUCCESS, targetUser);
    }

    private User findTargetUser(String userId) {
        return userRepository.findById(userId)
                .filter(u -> !u.isDeleted())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private FriendRelation findInRelations(List<FriendRelation> relations, String userId) {
        return relations.stream()
                .filter(r -> r.getRequester().getId().equals(userId))
                .findFirst().orElse(null);
    }

    private void validateNotBlockedByTarget(FriendRelation targetRelation) {
        if (targetRelation != null && targetRelation.getRelationStatus() == FriendRelationStatus.BLOCK) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }
    }

    private static boolean validateBySelf(User currentUser, User targetUser) {
        return currentUser.getId().equals(targetUser.getId());
    }
}
