package com.example.ForDay.domain.record.service.v2;

import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.record.dto.ReactionSummary;
import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.dto.request.RecordSearchConditionReqDto;
import com.example.ForDay.domain.record.dto.response.*;
import com.example.ForDay.domain.record.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordScrapRepository;
import com.example.ForDay.domain.record.type.ContextType;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.TimeUtil;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.util.S3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityRecordServiceV2 {
    private final ActivityRecordRepository activityRecordRepository;
    private final UserUtil userUtil;
    private final FriendRelationRepository friendRelationRepository;
    private final ActivityRecordReactionRepository recordReactionRepository;
    private final ActivityRecordScrapRepository activityRecordScrapRepository;
    private final S3Util s3Util;

    @Transactional(readOnly = true)
    public GetRecordDetailResDtoV2 getRecordDetailV2(Long recordId, RecordSearchConditionReqDto condition, CustomUserDetails user, List<Long> hobbyIds) {
        validateCondition(condition, hobbyIds);

        RecordDetailQueryDto detail = activityRecordRepository.findDetailDtoById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        // 삭제된 기록인지
        if (detail.recordDeleted()) throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);

        User currentUser = userUtil.getCurrentUser(user);
        String currentUserId = currentUser.getId();
        boolean isRecordOwner = Objects.equals(currentUserId, detail.writerId());

        // 권한 및 차단 체크
        checkBlockedAndDeletedUser(currentUserId, detail.writerId(), detail.writerDeleted());
        if (!isRecordOwner) {
            validateRecordAuthority(detail.visibility(), detail.writerId(), currentUserId);
        }

        // 리액션 및 스크랩 정보
        List<ReactionSummary> summaries = recordReactionRepository.findReactionSummariesByRecordId(recordId);
        GetRecordDetailResDtoV2.UserReactionDto userReaction = createUserReactionDto(summaries, currentUserId);
        GetRecordDetailResDtoV2.NewReactionDto newReaction = createNewReactionDto(summaries, isRecordOwner);
        boolean scraped = activityRecordScrapRepository.existsByScrap(detail.recordId(), currentUserId);

        // Context별 Prev/Next ID 조회
        Long prevId = activityRecordRepository.findPrevRecordId(recordId, detail.createdAt(), condition, currentUserId, hobbyIds);
        Long nextId = activityRecordRepository.findNextRecordId(recordId, detail.createdAt(), condition, currentUserId, hobbyIds);

        return buildGetRecordDetailResDtoV2(detail, isRecordOwner, newReaction, userReaction, scraped, prevId, nextId);
    }

    private GetRecordDetailResDtoV2 buildGetRecordDetailResDtoV2(
            RecordDetailQueryDto detail,
            boolean isRecordOwner,
            GetRecordDetailResDtoV2.NewReactionDto newReaction,
            GetRecordDetailResDtoV2.UserReactionDto userReaction,
            boolean scraped,
            Long prevId,
            Long nextId
    ) {
        return GetRecordDetailResDtoV2.builder()
                .hobbyId(detail.hobbyId())
                .hobbyName(detail.hobbyName())
                .activityId(detail.activityId())
                .activityContent(detail.activityContent())
                .activityRecordId(detail.recordId())
                .imageUrl(detail.imageUrl())
                .sticker(detail.sticker())
                .createdAt(TimeUtil.formatLocalDateTime(detail.createdAt()))
                .memo(detail.memo())
                .recordOwner(isRecordOwner)
                .scraped(scraped)
                .userInfo(GetRecordDetailResDtoV2.UserInfoDto.builder()
                        .userId(detail.writerId())
                        .nickname(detail.writerNickname())
                        .profileImageUrl(s3Util.toProfileMainResizedUrl(detail.writerProfileImageUrl()))
                        .build())
                .visibility(detail.visibility())
                .newReaction(newReaction)
                .userReaction(userReaction)
                .prevRecordId(prevId)
                .nextRecordId(nextId)
                .build();
    }

    private void validateCondition(RecordSearchConditionReqDto condition, List<Long> hobbyIds) {
        if (condition.context() == ContextType.STORY_HOBBY && hobbyIds.isEmpty()) {
            throw new CustomException(ErrorCode.HOBBY_ID_REQUIRED);
        }
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
        return friendRelationRepository.existsByFriendship(
                currentUserId, writerId, FriendRelationStatus.FOLLOW);
    }

    private GetRecordDetailResDtoV2.UserReactionDto createUserReactionDto(List<ReactionSummary> summaries, String userId) {
        List<RecordReactionType> myTypes = summaries.stream()
                .filter(s -> s.reactedUserId().equals(userId))
                .map(ReactionSummary::type).toList();
        return new GetRecordDetailResDtoV2.UserReactionDto(
                myTypes.contains(RecordReactionType.AWESOME),
                myTypes.contains(RecordReactionType.GREAT),
                myTypes.contains(RecordReactionType.AMAZING),
                myTypes.contains(RecordReactionType.FIGHTING)
        );
    }

    private GetRecordDetailResDtoV2.NewReactionDto createNewReactionDto(List<ReactionSummary> summaries, boolean isOwner) {
        if (!isOwner) return new GetRecordDetailResDtoV2.NewReactionDto(false, false, false, false);
        List<RecordReactionType> unreadTypes = summaries.stream()
                .filter(s -> !s.readWriter())
                .map(ReactionSummary::type).toList();
        return new GetRecordDetailResDtoV2.NewReactionDto(
                unreadTypes.contains(RecordReactionType.AWESOME),
                unreadTypes.contains(RecordReactionType.GREAT),
                unreadTypes.contains(RecordReactionType.AMAZING),
                unreadTypes.contains(RecordReactionType.FIGHTING)
        );
    }

    private void checkBlockedAndDeletedUser(String currentUserId, String targetId, boolean deleted) {
        // 한쪽이라도 차단 관계가 있는지 확인
        if (friendRelationRepository.existsByFriendship(currentUserId, targetId, FriendRelationStatus.BLOCK) || friendRelationRepository.existsByFriendship(targetId, currentUserId, FriendRelationStatus.BLOCK)) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }
        // 타겟유저가 탈퇴한 회원인 경우
        if (deleted) throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
    }
}