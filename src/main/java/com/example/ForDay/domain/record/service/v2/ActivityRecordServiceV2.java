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
import com.example.ForDay.global.util.ActivityRecordUtil;
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
    private static final String REACTION_QUEUE_VALUE_FORMAT = "%d:%s:%s";

    private final ActivityRecordRepository activityRecordRepository;
    private final UserUtil userUtil;
    private final ActivityRecordReactionRepository recordReactionRepository;
    private final ActivityRecordScrapRepository activityRecordScrapRepository;
    private final S3Util s3Util;
    private final RedisReactionService redisReactionService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ActivityRecordUtil activityRecordUtil;

    // 위, 아래 스와이프 적용 버전
    @Transactional(readOnly = true)
    public GetRecordDetailResDtoV2 getRecordDetailV2(Long recordId, RecordSearchConditionReqDto condition, CustomUserDetails user, List<Long> hobbyIds) {
        validateCondition(condition, hobbyIds);

        RecordDetailQueryDto detail = activityRecordRepository.findDetailDtoById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        if (detail.recordDeleted()) throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);

        User currentUser = userUtil.getCurrentUser(user);
        boolean isRecordOwner = activityRecordUtil.isRecordOwner(currentUser.getId(), detail.writerId());

        if (!isRecordOwner) {
            activityRecordUtil.validateAccess(currentUser.getId(), detail.writerId(), detail.writerDeleted(), detail.visibility());
        }

        List<ReactionSummary> summaries = recordReactionRepository.findReactionSummariesByRecordId(recordId);

        Long prevId = activityRecordRepository.findPrevRecordId(recordId, detail.createdAt(), condition, currentUser.getId(), hobbyIds);
        Long nextId = activityRecordRepository.findNextRecordId(recordId, detail.createdAt(), condition, currentUser.getId(), hobbyIds);

        return GetRecordDetailResDtoV2.of(detail, isRecordOwner, isScraped(detail, currentUser), prevId, nextId, summaries, s3Util.toProfileMainResizedUrl(detail.writerProfileImageUrl()));
    }

    @Transactional
    public ReactToRecordResDto reactToRecordWithRedis(Long recordId, RecordReactionType reactionType, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        ReportActivityRecordDto record = activityRecordUtil.getValidRecord(recordId);
        if (!activityRecordUtil.isRecordOwner(currentUser.getId(), record.getWriterId())) {
            activityRecordUtil.validateAccess(currentUser.getId(), record.getWriterId(), record.isWriterDeleted(), record.getVisibility());
        }
        validateDuplicateReactionWithRedis(recordId, currentUser.getId(), reactionType); // 메모리 많이 차지해서 삭제 예정

        // DB 저장 대신 Redis Queue에 push
        String value = REACTION_QUEUE_VALUE_FORMAT.formatted(
                recordId, currentUser.getId(), reactionType.name());
        redisTemplate.opsForList().rightPush("reaction_queue", value);

        // 랭킹 점수는 즉시 반영
        redisReactionService.incrementRankingScore(recordId);

        return new ReactToRecordResDto("반응이 정상적으로 등록되었습니다.", reactionType, recordId);
    }

    private boolean isScraped(RecordDetailQueryDto detail, User currentUser) {
        return activityRecordScrapRepository.existsByScrap(detail.recordId(), currentUser.getId());
    }

    private void validateCondition(RecordSearchConditionReqDto condition, List<Long> hobbyIds) {
        if (condition.context() == ContextType.STORY_HOBBY && hobbyIds.isEmpty()) {
            throw new CustomException(ErrorCode.HOBBY_ID_REQUIRED);
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