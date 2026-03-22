package com.example.ForDay.domain.record.service.v2;

import com.example.ForDay.domain.friend.entity.FriendRelation;
import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.reaction.entity.ActivityRecordReaction;
import com.example.ForDay.domain.reaction.entity.ActivityRecordReactionCount;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionCountRepository;
import com.example.ForDay.domain.record.dto.ReactionSummary;
import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.dto.ReportActivityRecordDto;
import com.example.ForDay.domain.record.dto.request.RecordSearchConditionReqDto;
import com.example.ForDay.domain.record.dto.response.*;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordScrapRepository;
import com.example.ForDay.domain.record.service.RedisReactionService;
import com.example.ForDay.domain.record.type.ContextType;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.TimeUtil;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.util.S3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
    private final RedisReactionService redisReactionService;
    private final RedisTemplate<String, String> redisTemplate;

    // 위, 아래 스와이프 적용 버전
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

    // 반응 생성 처리를 redis queue를 사용하여 처리
    @Transactional
    public ReactToRecordResDto reactToRecordWithRedis(Long recordId, RecordReactionType reactionType, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        ReportActivityRecordDto record = getValidRecord(recordId);
        validateAccess(record, currentUser);
        validateDuplicateReactionWithRedis(recordId, currentUser.getId(), reactionType);

        // DB 저장 대신 Redis Queue에 push
        String value = recordId + ":" + currentUser.getId() + ":" + reactionType.name();
        redisTemplate.opsForList().rightPush("reaction_queue", value);

        // 랭킹 점수는 즉시 반영
        updateRanking(recordId);

        return new ReactToRecordResDto("반응이 정상적으로 등록되었습니다.", reactionType, recordId);
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

    private void checkBlockedAndDeletedUser(List<FriendRelation> relations, String me, String target, boolean deleted) {
        // 리스트에서 차단(BLOCK)이 하나라도 있는지 확인
        boolean isBlocked = relations.stream()
                .anyMatch(f -> f.getRelationStatus() == FriendRelationStatus.BLOCK);

        if (isBlocked || deleted) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }
    }


    private ReportActivityRecordDto getValidRecord(Long recordId) {
        ReportActivityRecordDto record = activityRecordRepository.getReportActivityRecord(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        if (record.isRecordDeleted()) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }

        return record;
    }

    private void validateAccess(ReportActivityRecordDto record, User user) {
        String currentUserId = user.getId();

        if(!Objects.equals(currentUserId, record.getWriterId())) {
            List<FriendRelation> relations =
                    friendRelationRepository.findAllRelationsBetween(currentUserId, record.getWriterId());

            checkBlockedAndDeletedUser(
                    relations,
                    currentUserId,
                    record.getWriterId(),
                    record.isWriterDeleted()
            );

            validateRecordAuthority(
                    relations,
                    record.getVisibility(),
                    record.getWriterId(),
                    currentUserId
            );
        }
    }


    private void validateRecordAuthority(List<FriendRelation> relations, RecordVisibility visibility, String writerId, String me) {
        if (writerId.equals(me)) return;

        if (visibility == RecordVisibility.FRIEND) {
            boolean isFollowing = relations.stream()
                    .anyMatch(f -> f.getRequester().getId().equals(me) &&
                            f.getTargetUser().getId().equals(writerId) &&
                            f.getRelationStatus() == FriendRelationStatus.FOLLOW);

            if (!isFollowing) throw new CustomException(ErrorCode.FRIEND_ONLY_ACCESS);
        } else if (visibility == RecordVisibility.PRIVATE) {
            throw new CustomException(ErrorCode.PRIVATE_RECORD);
        }
    }

    private void updateRanking(Long recordId) {
        redisReactionService.incrementRankingScore(recordId);
    }

    private void validateDuplicateReaction(Long recordId, String userId, RecordReactionType type) {

        boolean exists = recordReactionRepository
                .existsByRecordIdAndUserIdAndType(recordId, userId, type);

        if (exists) {
            throw new CustomException(ErrorCode.DUPLICATE_REACTION);
        }
    }

    private void validateDuplicateReactionWithRedis(Long recordId, String userId, RecordReactionType type) {
        String key = "reaction:done:" + recordId + ":" + userId;

        // Set에 추가 시도 → 이미 있으면 0 반환 (중복)
        Boolean added = redisTemplate.opsForSet().add(key, type.name()) == 1L;

        if (!added) {
            throw new CustomException(ErrorCode.DUPLICATE_REACTION);
        }
    }
}