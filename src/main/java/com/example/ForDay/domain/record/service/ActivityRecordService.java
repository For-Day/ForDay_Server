package com.example.ForDay.domain.record.service;

import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.record.dto.ReactionSummary;
import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
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
        RecordDetailQueryDto detail = activityRecordRepository.findDetailDtoById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        String currentUserId = userUtil.getCurrentUser(user).getId();
        boolean isRecordOwner = Objects.equals(currentUserId, detail.writerId());

        if (!isRecordOwner) {
            validateRecordAuthority(detail.visibility(), detail.writerId(), currentUserId);
        }

        List<ReactionSummary> summaries = recordReactionRepository.findReactionSummariesByRecordId(recordId);


        GetRecordDetailResDto.UserReactionDto userReaction = createUserReactionDto(summaries, currentUserId);
        GetRecordDetailResDto.NewReactionDto newReaction = createNewReactionDto(summaries, isRecordOwner);

        return buildGetRecordDetailResDtoFromDto(detail, isRecordOwner, newReaction, userReaction);
    }

    @Transactional
    public GetRecordReactionUsersResDto getRecordReactionUsers(
            Long recordId, RecordReactionType type, CustomUserDetails user, String lastUserId, Integer size
    ) {
        // 1. 엔티티 전체 대신 권한 확인용 DTO만 조회 (Fetch Join 제거 효과)
        RecordDetailQueryDto recordDetail = activityRecordRepository.findDetailDtoById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        String currentUserId = userUtil.getCurrentUser(user).getId();
        boolean isRecordOwner = Objects.equals(currentUserId, recordDetail.writerId());

        // 2. 이미 가져온 DTO 정보로 권한 검증
        validateRecordAuthority(recordDetail.visibility(), recordDetail.writerId(), currentUserId);

        // 3. 리포지토리에서 DTO(ReactionUserInfo)로 직접 조회하여 N+1 및 오버페칭 방지
        List<GetRecordReactionUsersResDto.ReactionUserInfo> reactionUsers =
                recordReactionRepository.findReactionUsersDtoByType(recordId, type, lastUserId, size, isRecordOwner);

        // 4. 다음 페이지 여부 확인
        boolean hasNext = reactionUsers.size() > size;
        if (hasNext) reactionUsers.remove(size.intValue());

        // 5. 게시글 주인인 경우에만 벌크 업데이트 실행
        if (isRecordOwner) {
            recordReactionRepository.markAsReadByRecordIdAndType(recordId, type);
        }

        String nextLastUserId = reactionUsers.isEmpty() ? null : reactionUsers.get(reactionUsers.size() - 1).getUserId();

        return new GetRecordReactionUsersResDto(type, reactionUsers, hasNext, nextLastUserId);
    }

    @Transactional
    public ReactToRecordResDto reactToRecord(Long recordId, RecordReactionType type, CustomUserDetails user) {
        ActivityRecord activityRecord = getActivityRecord(recordId);
        User currentUser = userUtil.getCurrentUser(user);

        validateRecordAuthority(activityRecord.getVisibility(), activityRecord.getUser().getId(), currentUser.getId());

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

    private void validateRecordAuthority(RecordVisibility visibility, String writerId, String currentUserId) {
        if (writerId.equals(currentUserId)) return;

        switch (visibility) {
            case FRIEND -> {
                if (!checkFriendship(writerId, currentUserId)) throw new CustomException(ErrorCode.FRIEND_ONLY_ACCESS);
            }
            case PRIVATE -> throw new CustomException(ErrorCode.PRIVATE_RECORD);
            default -> {
            } // PUBLIC
        }
    }

    private boolean checkFriendship(String writerId, String currentUserId) {
        return friendRelationRepository.existsAcceptedFriendship(writerId, currentUserId);
    }

    private void verifyRecordOwner(ActivityRecord record, User user) {
        if (!Objects.equals(record.getUser(), user)) {
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

    private GetRecordDetailResDto buildGetRecordDetailResDtoFromDto(RecordDetailQueryDto detail,
                                                                    boolean isOwner,
                                                                    GetRecordDetailResDto.NewReactionDto newR,
                                                                    GetRecordDetailResDto.UserReactionDto userR) {
        return GetRecordDetailResDto.builder()
                .activityContent(detail.activityContent())
                .activityRecordId(detail.recordId())
                .imageUrl(detail.imageUrl())
                .sticker(detail.sticker())
                .createdAt(TimeUtil.formatLocalDateTime(detail.createdAt()))
                .memo(detail.memo())
                .recordOwner(isOwner)
                .visibility(detail.visibility())
                .newReaction(newR)
                .userReaction(userR)
                .build();
    }
}