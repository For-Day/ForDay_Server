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

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {
    private final FriendRelationRepository friendRelationRepository;
    private final UserUtil userUtil;
    private final UserRepository userRepository;
    private final S3Util s3Util;

    @Transactional
    public AddFriendResDto addFriend(AddFriendReqDto reqDto, CustomUserDetails userDetails) {
        User currentUser = userUtil.getCurrentUser(userDetails);
        User targetUser = findTargetUser(reqDto.getUserId());

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new CustomException(ErrorCode.CANNOT_FOLLOW_SELF);
        }

        // 양방향 관계 조회 및 할당
        List<FriendRelation> relations = friendRelationRepository.findBothDirections(currentUser.getId(), targetUser.getId());
        FriendRelation myRelation = findInRelations(relations, currentUser.getId());
        FriendRelation targetRelation = findInRelations(relations, targetUser.getId());

        // 상대가 나를 차단했는지 확인
        validateNotBlockedByTarget(targetRelation);

        // 내 관계 처리
        if (myRelation != null) {
            FriendRelationStatus status = myRelation.getRelationStatus();
            if (status == FriendRelationStatus.FOLLOW) {
                return new AddFriendResDto("이미 친구 맺기가 되어있습니다.", targetUser.getNickname());
            }
            // 내가 차단/신고한 유저에게는 친구 추가 불가
            if (status == FriendRelationStatus.BLOCK || status == FriendRelationStatus.REPORT) {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }
            myRelation.changeStatus(FriendRelationStatus.FOLLOW);
        } else {
            saveRelation(currentUser, targetUser, FriendRelationStatus.FOLLOW);
        }

        return new AddFriendResDto("성공적으로 친구 맺기가 되었습니다.", targetUser.getNickname());
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
        return new DeleteFriendResDto("성공적으로 친구 관계를 삭제했습니다.", targetUser.getNickname());
    }

    @Transactional
    public BlockFriendResDto blockFriend(BlockFriendReqDto reqDto, CustomUserDetails userDetails) {
        User currentUser = userUtil.getCurrentUser(userDetails);
        User targetUser = findTargetUser(reqDto.getUserId());

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new CustomException(ErrorCode.CANNOT_BLOCK_SELF);
        }

        List<FriendRelation> relations = friendRelationRepository.findBothDirections(currentUser.getId(), targetUser.getId());
        FriendRelation myRelation = findInRelations(relations, currentUser.getId());
        FriendRelation targetRelation = findInRelations(relations, targetUser.getId());

        validateNotBlockedByTarget(targetRelation);

        if (myRelation != null) {
            if (myRelation.getRelationStatus() == FriendRelationStatus.BLOCK) {
                return new BlockFriendResDto("이미 차단된 상태입니다.", targetUser.getNickname());
            }
            myRelation.changeStatus(FriendRelationStatus.BLOCK);
        } else {
            saveRelation(currentUser, targetUser, FriendRelationStatus.BLOCK);
        }

        return new BlockFriendResDto(targetUser.getNickname() + "님이 차단되었어요.", targetUser.getNickname());
    }

    @Transactional(readOnly = true)
    public GetFriendListResDto getFriendList(CustomUserDetails user, String lastUserId, Integer size) {
        User currentUser = userUtil.getCurrentUser(user);
        List<GetFriendListResDto.UserInfoDto> userInfoDtos = friendRelationRepository.findMyFriendList(currentUser.getId(), lastUserId, size + 1);

        boolean hasNext = userInfoDtos.size() > size;
        if (hasNext) {
            userInfoDtos = userInfoDtos.subList(0, size);
        }

        List<GetFriendListResDto.UserInfoDto> updatedList = userInfoDtos.stream()
                .map(dto -> new GetFriendListResDto.UserInfoDto(
                        dto.getUserId(),
                        dto.getNickname(),
                        s3Util.toProfileMainResizedUrl(dto.getProfileImageUrl())
                ))
                .toList();

        String nextLastUserId = updatedList.isEmpty() ? null : updatedList.get(updatedList.size() - 1).getUserId();
        return new GetFriendListResDto("친구 목록이 성공적으로 조회되었습니다.", updatedList, nextLastUserId, hasNext);
    }

    @Transactional
    public ReportFriendResDto reportFriend(ReportFriendReqDto reqDto, CustomUserDetails userDetails) {
        User currentUser = userUtil.getCurrentUser(userDetails);
        User targetUser = findTargetUser(reqDto.getUserId());

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new CustomException(ErrorCode.CANNOT_REPORT_SELF);
        }

        List<FriendRelation> relations = friendRelationRepository.findBothDirections(currentUser.getId(), targetUser.getId());
        FriendRelation myRelation = findInRelations(relations, currentUser.getId());
        FriendRelation targetRelation = findInRelations(relations, targetUser.getId());

        validateNotBlockedByTarget(targetRelation);

        if (myRelation != null) {
            FriendRelationStatus status = myRelation.getRelationStatus();
            if (status == FriendRelationStatus.REPORT) {
                return new ReportFriendResDto("이미 신고된 상태입니다.", targetUser.getNickname(), targetUser.getId());
            } else if (status == FriendRelationStatus.BLOCK) {
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }
            myRelation.changeStatus(FriendRelationStatus.REPORT);
        } else {
            saveRelation(currentUser, targetUser, FriendRelationStatus.REPORT);
        }

        return new ReportFriendResDto("신고가 완료되었습니다.", targetUser.getNickname(), targetUser.getId());
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

    private void saveRelation(User requester, User target, FriendRelationStatus status) {
        friendRelationRepository.save(FriendRelation.builder()
                .requester(requester)
                .targetUser(target)
                .relationStatus(status)
                .build());
    }
}
