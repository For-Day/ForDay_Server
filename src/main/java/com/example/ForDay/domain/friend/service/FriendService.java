package com.example.ForDay.domain.friend.service;

import com.example.ForDay.domain.friend.dto.request.AddFriendReqDto;
import com.example.ForDay.domain.friend.dto.request.BlockFriendReqDto;
import com.example.ForDay.domain.friend.dto.response.AddFriendResDto;
import com.example.ForDay.domain.friend.dto.response.BlockFriendResDto;
import com.example.ForDay.domain.friend.dto.response.DeleteFriendResDto;
import com.example.ForDay.domain.friend.dto.response.GetFriendListResDto;
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
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FriendService {
    private final FriendRelationRepository friendRelationRepository;
    private final UserUtil userUtil;
    private final UserRepository userRepository;
    private final S3Util s3Util;

    @Transactional
    public AddFriendResDto addFriend(AddFriendReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        User targetUser = userRepository.findById(reqDto.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 유저가 탈퇴한 경우
        if(targetUser.isDeleted()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        String currentUserId = currentUser.getId();
        String targetUserId = targetUser.getId();

        log.info("[addFriend] 친구 추가 시도: {} -> {}", currentUserId, targetUserId);

        // 자기 자신 체크
        if (currentUserId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.CANNOT_FOLLOW_SELF);
        }

        // 상대방이 나를 차단했는지 확인
        Optional<FriendRelation> blockedByTarget = friendRelationRepository
                .findByRequesterIdAndTargetUserId(targetUserId, currentUserId);

        if (blockedByTarget.isPresent() && blockedByTarget.get().getRelationStatus() == FriendRelationStatus.BLOCK) {
            log.info("[addFriend] 상대방이 나를 차단함. 요청 거부: {}", targetUserId);
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 내가 맺은 관계 확인
        Optional<FriendRelation> myRelation = friendRelationRepository
                .findByRequesterIdAndTargetUserId(currentUserId, targetUserId);

        if (myRelation.isPresent()) {
            FriendRelation relation = myRelation.get();
            if (relation.getRelationStatus() == FriendRelationStatus.FOLLOW) {
                log.info("[addFriend] 이미 친구 상태임: {} -> {}", currentUserId, targetUserId);
                return new AddFriendResDto("이미 친구 맺기가 되어있습니다.", targetUser.getNickname());
            } else if (relation.getRelationStatus() == FriendRelationStatus.BLOCK) {
                log.warn("[addFriend] 본인이 차단한 유저에게 친구 요청 시도: {}", targetUserId);
                throw new CustomException(ErrorCode.USER_NOT_FOUND);
            }
        }

        // 새로운 친구 관계 생성
        friendRelationRepository.save(FriendRelation.builder()
                .requester(currentUser)
                .targetUser(targetUser)
                .relationStatus(FriendRelationStatus.FOLLOW)
                .build());

        log.info("[addFriend] 친구 추가 성공: {} -> {}", currentUserId, targetUser.getNickname());
        return new AddFriendResDto("성공적으로 친구 맺기가 되었습니다.", targetUser.getNickname());
    }

    @Transactional
    public DeleteFriendResDto deleteFriend(String friendId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[deleteFriend] 친구 관계 삭제 시작: {} -> {}", currentUser.getId(), friendId);

        User targetUser = userRepository.findById(friendId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if(targetUser.isDeleted()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        String currentUserId = currentUser.getId();
        String targetUserId = targetUser.getId();

        // 내가 맺은 관계 조회 (나 -> 상대)
        FriendRelation myRelation = friendRelationRepository
                .findByRequesterIdAndTargetUserId(currentUserId, targetUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.FRIEND_NOT_FOUND));

        // 현재 상태가 FOLLOW일 때만 삭제 가능. 만약 내가 상대를 BLOCK 중이라면 삭제(언팔로우) 대상이 아님.
        if (myRelation.getRelationStatus() != FriendRelationStatus.FOLLOW) {
            throw new CustomException(ErrorCode.FRIEND_NOT_FOUND);
        }

        // 상대방이 나를 차단했는지 확인
        Optional<FriendRelation> blockedByTarget = friendRelationRepository
                .findByRequesterIdAndTargetUserId(targetUserId, currentUserId);

        if (blockedByTarget.isPresent() && blockedByTarget.get().getRelationStatus() == FriendRelationStatus.BLOCK) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        friendRelationRepository.delete(myRelation);

        log.info("[deleteFriend] 친구 관계 삭제 완료: {} -> {}", currentUser.getId(), targetUser.getNickname());
        return new DeleteFriendResDto("성공적으로 친구 관계를 삭제했습니다.", targetUser.getNickname());
    }

    @Transactional
    public BlockFriendResDto blockFriend(BlockFriendReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        User targetUser = userRepository.findById(reqDto.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if(targetUser.isDeleted()) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        String currentUserId = currentUser.getId();
        String targetUserId = targetUser.getId();

        log.info("[blockFriend] 차단 프로세스 시작: {} -> {}", currentUser.getId(), targetUserId);

        if (currentUserId.equals(targetUserId)) {
            throw new CustomException(ErrorCode.CANNOT_BLOCK_SELF);
        }

        // 상대방이 나를 차단했는지 확인
        Optional<FriendRelation> blockedByTarget = friendRelationRepository
                .findByRequesterIdAndTargetUserId(targetUserId, currentUserId);

        if (blockedByTarget.isPresent() && blockedByTarget.get().getRelationStatus() == FriendRelationStatus.BLOCK) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        // 나의 차단 로직 수행
        Optional<FriendRelation> myRelation = friendRelationRepository
                .findByRequesterIdAndTargetUserId(currentUserId, targetUserId);

        if (myRelation.isPresent()) {
            FriendRelation relation = myRelation.get();

            if (relation.getRelationStatus() == FriendRelationStatus.BLOCK) {
                log.info("[blockFriend] 이미 차단된 상태임: {}", targetUserId);
                return new BlockFriendResDto("이미 차단된 상태입니다.", targetUser.getNickname());
            }

            // FOLLOW 상태에서 BLOCK으로 변경
            relation.changeStatus(FriendRelationStatus.BLOCK);
            log.info("[blockFriend] FOLLOW -> BLOCK 상태 변경 완료: {}", targetUserId);
        } else {
            friendRelationRepository.save(FriendRelation.builder()
                    .requester(currentUser)
                    .targetUser(targetUser)
                    .relationStatus(FriendRelationStatus.BLOCK)
                    .build());
            log.info("[blockFriend] 신규 BLOCK 관계 생성 완료: {}", targetUserId);
        }

        return new BlockFriendResDto("성공적으로 차단되었습니다.", targetUser.getNickname());
    }

    @Transactional(readOnly = true)
    public GetFriendListResDto getFriendList(CustomUserDetails user, String lastUserId, Integer size) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[getFriendList] 친구 목록 조회 시작 - User: {}, lastUserId: {}, size: {}",
                currentUser.getId(), lastUserId, size);

        List<GetFriendListResDto.UserInfoDto> userInfoDtos = friendRelationRepository.findMyFriendList(currentUser.getId(), lastUserId, size);

        boolean hasNext = false;
        if (userInfoDtos.size() > size) {
            hasNext = true;
            userInfoDtos.remove(size.intValue());
        }

        String nextLastUserId = userInfoDtos.isEmpty() ? null :
                userInfoDtos.get(userInfoDtos.size() - 1).getUserId();

        List<GetFriendListResDto.UserInfoDto> updatedList = userInfoDtos.stream()
                .map(dto -> new GetFriendListResDto.UserInfoDto(
                        dto.getUserId(),
                        dto.getNickname(),
                        s3Util.toProfileMainResizedUrl(dto.getProfileImageUrl()) // 여기서 변환 처리
                ))
                .toList();

        log.info("[getFriendList] 조회 완료 - 반환 데이터 개수: {}개, 다음 페이지 존재여부: {}",
                userInfoDtos.size(), (userInfoDtos.size() >= size));

        return new GetFriendListResDto("친구 목록이 성공적으로 조회되었습니다.", updatedList, nextLastUserId, hasNext);
    }
}
