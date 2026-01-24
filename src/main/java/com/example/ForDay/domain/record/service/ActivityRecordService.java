package com.example.ForDay.domain.record.service;

import com.example.ForDay.domain.friend.FriendRelationRepository;
import com.example.ForDay.domain.record.dto.request.UpdateRecordVisibilityReqDto;
import com.example.ForDay.domain.record.dto.response.*;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.ActivityRecordReaction;
import com.example.ForDay.domain.record.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.TimeUtil;
import com.example.ForDay.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityRecordService {
    private final ActivityRecordRepository activityRecordRepository;
    private final UserUtil userUtil;
    private final FriendRelationRepository friendRelationRepository;
    private final ActivityRecordReactionRepository recordReactionRepository;

    @Transactional
    public GetRecordDetailResDto getRecordDetail(Long recordId, CustomUserDetails user) {
        ActivityRecord activityRecord = getActivityRecord(recordId);
        User currentUser = userUtil.getCurrentUser(user);
        User writer = activityRecord.getUser();
        String currentUserId = currentUser.getId();

        // 내가 이 글에 누른 리액션 정보 (UserReactionDto)
        List<RecordReactionType> myReactions = recordReactionRepository.findAllMyReactions(recordId, currentUserId);
        GetRecordDetailResDto.UserReactionDto userReaction = new GetRecordDetailResDto.UserReactionDto(
                myReactions.contains(RecordReactionType.AWESOME),
                myReactions.contains(RecordReactionType.GREAT),
                myReactions.contains(RecordReactionType.AMAZING),
                myReactions.contains(RecordReactionType.FIGHTING)
        );

        GetRecordDetailResDto.NewReactionDto newReaction;

        boolean isRecordOwner = false;
        // 권한 판별 및 New 알림 처리
        if (Objects.equals(currentUserId, writer.getId())) {
            isRecordOwner = true;

            //  읽지 않은 리액션이 있는지 확인
            List<RecordReactionType> unreadTypes = recordReactionRepository.findAllUnreadReactions(recordId);
            newReaction = new GetRecordDetailResDto.NewReactionDto(
                    unreadTypes.contains(RecordReactionType.AWESOME),
                    unreadTypes.contains(RecordReactionType.GREAT),
                    unreadTypes.contains(RecordReactionType.AMAZING),
                    unreadTypes.contains(RecordReactionType.FIGHTING)
            );

            // 확인한 리액션들은 모두 읽음 처리 (Dirty Checking)
            recordReactionRepository.markAsReadByRecordId(recordId);
        } else {
            //  권한 체크
            validateVisibility(activityRecord, writer, currentUser);
            // 타인 조회 시 new 정보는 항상 false
            newReaction = new GetRecordDetailResDto.NewReactionDto(false, false, false, false);
        }

        return GetRecordDetailResDto.builder()
                .activityId(activityRecord.getActivity().getId())
                .activityContent(activityRecord.getActivity().getContent())
                .activityRecordId(activityRecord.getId())
                .imageUrl(activityRecord.getImageUrl())
                .sticker(activityRecord.getSticker())
                .createdAt(TimeUtil.formatLocalDateTime(activityRecord.getCreatedAt()))
                .memo(activityRecord.getMemo())
                .recordOwner(isRecordOwner)
                .visibility(activityRecord.getVisibility())
                .newReaction(newReaction)
                .userReaction(userReaction)
                .build();
    }

    private void validateVisibility(ActivityRecord record, User writer, User currentUser) {
        switch (record.getVisibility()) {
            case FRIEND -> {
                if (!checkFriendship(writer, currentUser)) {
                    throw new CustomException(ErrorCode.FRIEND_ONLY_ACCESS);
                }
            }
            case PRIVATE -> throw new CustomException(ErrorCode.PRIVATE_RECORD);
            case PUBLIC -> {} // 통과
        }
    }

    private boolean checkFriendship(User writer, User currentUser) {
        if (writer.getId().equals(currentUser.getId())) {
            return true;
        }

        return friendRelationRepository.existsAcceptedFriendship(
                writer.getId(),
                currentUser.getId()
        );
    }

    private ActivityRecord getActivityRecord(Long activityRecordId) {
        return activityRecordRepository.findById(activityRecordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));
    }

    @Transactional
    public UpdateRecordVisibilityResDto updateRecordVisibility(Long recordId, UpdateRecordVisibilityReqDto reqDto, CustomUserDetails user) {
        ActivityRecord activityRecord = getActivityRecord(recordId);
        User currentUser = userUtil.getCurrentUser(user);
        verifyRecordOwner(activityRecord, currentUser);

        RecordVisibility previousVisibility = activityRecord.getVisibility();
        RecordVisibility newVisibility = reqDto.getVisibility();

        if (previousVisibility.equals(newVisibility)) {
            return new UpdateRecordVisibilityResDto("이미 설정된 공개 범위입니다.", previousVisibility, newVisibility);
        }
        activityRecord.updateVisibility(newVisibility);

        return new UpdateRecordVisibilityResDto("공개 범위가 정상적으로 변경되었습니다.", previousVisibility, newVisibility);
    }

    private static void verifyRecordOwner(ActivityRecord activityRecord, User currentUser) {
        if (!activityRecord.getUser().getId().equals(currentUser.getId())) {
            throw new CustomException(ErrorCode.NOT_ACTIVITY_RECORD_OWNER);
        }
    }

    @Transactional(readOnly = true)
    public GetRecordReactionUsersResDto getRecordReactionUsers(Long recordId, RecordReactionType reactionType, CustomUserDetails user) {
        ActivityRecord activityRecord = getActivityRecord(recordId);
        User currentUser = userUtil.getCurrentUser(user);

        // 소유자 권한 검증
        verifyRecordOwner(activityRecord, currentUser);

        // Repository 호출 (Querydsl: readWriter=false, 최신순)
        List<ActivityRecordReaction> unreadReactions =
                recordReactionRepository.findUnreadReactionsByType(recordId, reactionType);

        // DTO 변환
        List<GetRecordReactionUsersResDto.ReactionUserInfo> users = unreadReactions.stream()
                .map(reaction -> new GetRecordReactionUsersResDto.ReactionUserInfo(
                        reaction.getReactedUser().getId(),
                        reaction.getReactedUser().getNickname(),
                        reaction.getReactedUser().getProfileImageUrl(),
                        reaction.getCreatedAt()
                ))
                .collect(Collectors.toList());

        return new GetRecordReactionUsersResDto(reactionType, users);
    }

    @Transactional
    public ReactToRecordResDto reactToRecord(Long recordId, RecordReactionType reactionType, CustomUserDetails user) {
        ActivityRecord activityRecord = getActivityRecord(recordId);
        User writer = activityRecord.getUser();
        User currentUser = userUtil.getCurrentUser(user);

        // 반응 권한 확인 (본인이거나, 전체공개이거나, 친구인 경우)
        validateReactionAuthority(activityRecord, writer, currentUser);

        // 이미 해당 타입의 리액션을 남겼는지 확인 (중복 방지)
        Optional<ActivityRecordReaction> existingReaction =
                recordReactionRepository.findByActivityRecordAndReactedUserAndReactionType(activityRecord, currentUser, reactionType);

        if (existingReaction.isPresent()) {
            throw new CustomException(ErrorCode.DUPLICATE_REACTION);
        }

        // 리액션 저장
        ActivityRecordReaction reaction = ActivityRecordReaction.builder()
                .activityRecord(activityRecord)
                .reactedUser(currentUser)
                .reactionType(reactionType)
                .readWriter(false)
                .build();

        recordReactionRepository.save(reaction);

        return new ReactToRecordResDto("반응이 정상적으로 등록되었습니다.", reactionType, recordId);
    }

    private void validateReactionAuthority(ActivityRecord record, User writer, User currentUser) {
        // 본인 글에는 항상 반응 가능
        if (writer.getId().equals(currentUser.getId())) {
            return;
        }

        switch (record.getVisibility()) {
            case PUBLIC -> {
                // 전체 공개글은 누구나 가능
            }
            case FRIEND -> {
                // 친구 관계인지 확인
                if (!checkFriendship(writer, currentUser)) {
                    throw new CustomException(ErrorCode.FRIEND_ONLY_ACCESS);
                }
            }
            case PRIVATE -> {
                // 나만 보기 글은 본인 외에 반응 불가
                throw new CustomException(ErrorCode.PRIVATE_RECORD);
            }
        }
    }

    public CancelReactToRecordResDto cancelReactToRecord(Long recordId, RecordReactionType reactionType, CustomUserDetails user) {
        ActivityRecord activityRecord = getActivityRecord(recordId);
        User currentUser = userUtil.getCurrentUser(user);

        ActivityRecordReaction reaction = recordReactionRepository
                .findByActivityRecordAndReactedUserAndReactionType(activityRecord, currentUser, reactionType)
                .orElseThrow(() -> new CustomException(ErrorCode.REACTION_NOT_FOUND));

        recordReactionRepository.delete(reaction);

        return new CancelReactToRecordResDto("리액션이 정상적으로 취소되었습니다.", reactionType, recordId);
    }
}
