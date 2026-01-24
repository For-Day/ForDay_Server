package com.example.ForDay.domain.record.service;

import com.example.ForDay.domain.friend.FriendRelationRepository;
import com.example.ForDay.domain.record.dto.ReactionSummary;
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

@Service
@RequiredArgsConstructor
public class ActivityRecordService {
    private final ActivityRecordRepository activityRecordRepository;
    private final UserUtil userUtil;
    private final FriendRelationRepository friendRelationRepository;
    private final ActivityRecordReactionRepository recordReactionRepository;

    @Transactional(readOnly = true)
    public GetRecordDetailResDto getRecordDetail(Long recordId, CustomUserDetails user) {
        ActivityRecord activityRecord = activityRecordRepository.findByIdWithUserAndActivity(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        User currentUser = userUtil.getCurrentUser(user);
        User writer = activityRecord.getUser();
        boolean isRecordOwner = Objects.equals(currentUser.getId(), writer.getId());

        if (!isRecordOwner) {
            validateRecordAuthority(activityRecord, writer, currentUser);
        }

        List<ReactionSummary> summaries = recordReactionRepository.findReactionSummariesByRecordId(recordId);

        GetRecordDetailResDto.UserReactionDto userReaction = createUserReactionDto(summaries, currentUser.getId());
        GetRecordDetailResDto.NewReactionDto newReaction = createNewReactionDto(summaries, isRecordOwner);

        return buildGetRecordDetailResDto(activityRecord, isRecordOwner, newReaction, userReaction);
    }

    @Transactional
    public GetRecordReactionUsersResDto getRecordReactionUsers(
            Long recordId, RecordReactionType type, CustomUserDetails user, String lastUserId, Integer size
    ) {
        ActivityRecord activityRecord = getActivityRecord(recordId);
        User currentUser = userUtil.getCurrentUser(user);
        User writer = activityRecord.getUser();
        boolean isRecordOwner = Objects.equals(currentUser.getId(), writer.getId());

        validateRecordAuthority(activityRecord, writer, currentUser);

        List<ActivityRecordReaction> reactions = recordReactionRepository.findUsersReactionsByType(recordId, type, lastUserId, size);

        boolean hasNext = reactions.size() > size;
        if (hasNext) reactions.remove(size.intValue());

        List<GetRecordReactionUsersResDto.ReactionUserInfo> reactionUsers = reactions.stream()
                .map(r -> new GetRecordReactionUsersResDto.ReactionUserInfo(
                        r.getReactedUser().getId(),
                        r.getReactedUser().getNickname(),
                        r.getReactedUser().getProfileImageUrl(),
                        r.getCreatedAt(),
                        isRecordOwner && !r.isReadWriter()
                )).toList();

        if (isRecordOwner) {
            recordReactionRepository.markAsReadByRecordIdAndType(recordId, type);
        }

        return new GetRecordReactionUsersResDto(type, reactionUsers, hasNext, reactionUsers.get(reactionUsers.size() - 1).getUserId());
    }

    @Transactional
    public ReactToRecordResDto reactToRecord(Long recordId, RecordReactionType type, CustomUserDetails user) {
        ActivityRecord activityRecord = getActivityRecord(recordId);
        User currentUser = userUtil.getCurrentUser(user);

        validateRecordAuthority(activityRecord, activityRecord.getUser(), currentUser);

        if (recordReactionRepository.existsByActivityRecordAndReactedUserAndReactionType(activityRecord, currentUser, type)) {
            throw new CustomException(ErrorCode.DUPLICATE_REACTION);
        }

        recordReactionRepository.save(ActivityRecordReaction.builder()
                .activityRecord(activityRecord)
                .reactedUser(currentUser)
                .reactionType(type)
                .readWriter(false)
                .build());

        return new ReactToRecordResDto("반응이 정상적으로 등록되었습니다.", type, recordId);
    }

    @Transactional
    public UpdateRecordVisibilityResDto updateRecordVisibility(Long recordId, UpdateRecordVisibilityReqDto reqDto, CustomUserDetails user) {
        ActivityRecord activityRecord = getActivityRecord(recordId);
        verifyRecordOwner(activityRecord, userUtil.getCurrentUser(user));

        RecordVisibility previous = activityRecord.getVisibility();
        RecordVisibility next = reqDto.getVisibility();

        if (previous == next) {
            return new UpdateRecordVisibilityResDto("이미 설정된 공개 범위입니다.", previous, next);
        }

        activityRecord.updateVisibility(next);
        return new UpdateRecordVisibilityResDto("공개 범위가 정상적으로 변경되었습니다.", previous, next);
    }

    @Transactional
    public CancelReactToRecordResDto cancelReactToRecord(Long recordId, RecordReactionType type, CustomUserDetails user) {
        ActivityRecord activityRecord = getActivityRecord(recordId);
        ActivityRecordReaction reaction = recordReactionRepository
                .findByActivityRecordAndReactedUserAndReactionType(activityRecord, userUtil.getCurrentUser(user), type)
                .orElseThrow(() -> new CustomException(ErrorCode.REACTION_NOT_FOUND));

        recordReactionRepository.delete(reaction);
        return new CancelReactToRecordResDto("리액션이 정상적으로 취소되었습니다.", type, recordId);
    }

    private void validateRecordAuthority(ActivityRecord record, User writer, User currentUser) {
        if (writer.getId().equals(currentUser.getId())) return;

        switch (record.getVisibility()) {
            case FRIEND -> {
                if (!checkFriendship(writer, currentUser)) throw new CustomException(ErrorCode.FRIEND_ONLY_ACCESS);
            }
            case PRIVATE -> throw new CustomException(ErrorCode.PRIVATE_RECORD);
            default -> {} // PUBLIC
        }
    }

    private boolean checkFriendship(User writer, User currentUser) {
        return friendRelationRepository.existsAcceptedFriendship(writer.getId(), currentUser.getId());
    }

    private void verifyRecordOwner(ActivityRecord record, User user) {
        if (!record.getUser().getId().equals(user.getId())) {
            throw new CustomException(ErrorCode.NOT_ACTIVITY_RECORD_OWNER);
        }
    }

    private ActivityRecord getActivityRecord(Long id) {
        return activityRecordRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));
    }

    private GetRecordDetailResDto.UserReactionDto createUserReactionDto(List<ReactionSummary> summaries, String userId) {
        List<RecordReactionType> myTypes = summaries.stream()
                .filter(s -> s.reactedUserId().equals(userId))
                .map(ReactionSummary::type).toList();
        return new GetRecordDetailResDto.UserReactionDto(
                myTypes.contains(RecordReactionType.AWESOME),
                myTypes.contains(RecordReactionType.GREAT),
                myTypes.contains(RecordReactionType.AMAZING),
                myTypes.contains(RecordReactionType.FIGHTING)
        );
    }

    private GetRecordDetailResDto.NewReactionDto createNewReactionDto(List<ReactionSummary> summaries, boolean isOwner) {
        if (!isOwner) return new GetRecordDetailResDto.NewReactionDto(false, false, false, false);
        List<RecordReactionType> unreadTypes = summaries.stream()
                .filter(s -> !s.readWriter())
                .map(ReactionSummary::type).toList();
        return new GetRecordDetailResDto.NewReactionDto(
                unreadTypes.contains(RecordReactionType.AWESOME),
                unreadTypes.contains(RecordReactionType.GREAT),
                unreadTypes.contains(RecordReactionType.AMAZING),
                unreadTypes.contains(RecordReactionType.FIGHTING)
        );
    }

    private GetRecordDetailResDto buildGetRecordDetailResDto(ActivityRecord record, boolean isOwner,
                                                             GetRecordDetailResDto.NewReactionDto newR,
                                                             GetRecordDetailResDto.UserReactionDto userR) {
        return GetRecordDetailResDto.builder()
                .activityId(record.getActivity().getId())
                .activityContent(record.getActivity().getContent())
                .activityRecordId(record.getId())
                .imageUrl(record.getImageUrl())
                .sticker(record.getSticker())
                .createdAt(TimeUtil.formatLocalDateTime(record.getCreatedAt()))
                .memo(record.getMemo())
                .recordOwner(isOwner)
                .visibility(record.getVisibility())
                .newReaction(newR)
                .userReaction(userR)
                .build();
    }
}