package com.example.ForDay.domain.reaction.service;

import com.example.ForDay.domain.record.dto.ReportActivityRecordDto;
import com.example.ForDay.domain.record.dto.response.ReactToRecordResDto;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.record.utils.ActivityRecordUtil;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.constants.CacheConstants;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 이슈 #375 4단계 재측정의 3단계(Redis Write-Back 큐만 적용, 분산 락은 아직 없음) 전용.
 * {@link ReactionRedisLockService#createReactionWithRedis}에서 SETNX 락 체크만 빼고
 * 중복확인을 v1과 동일하게 DB {@code existsBy...} 조회로 되돌린 버전이다 — 큐 드레인은
 * {@link ReactionScheduler}를 그대로 재사용한다(큐에 들어간 {@code userId:recordId:type}
 * 포맷만 보고 처리하므로 push하는 쪽 로직과 무관하다).
 *
 * <p>{@code measure} 프로파일에서만 빈으로 등록되어 프로덕션에는 존재하지 않는다.
 */
@Service
@Profile("measure")
@RequiredArgsConstructor
public class QueueOnlyReactionMeasurementService {
    private final UserUtil userUtil;
    private final ActivityRecordUtil activityRecordUtil;
    private final ActivityRecordReactionRepository recordReactionRepository;
    private final ReactionRankingService reactionRankingService;
    private final RedisTemplate<String, String> redisTemplate;

    public ReactToRecordResDto reactToRecordQueueOnly(Long recordId, RecordReactionType type, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        ReportActivityRecordDto record = activityRecordUtil.getValidRecord(recordId);

        if (!activityRecordUtil.isRecordOwner(currentUser.getId(), record.getWriterId())) {
            activityRecordUtil.validateAccess(currentUser.getId(), record.getWriterId(), record.isWriterDeleted(), record.getVisibility());
        }

        if (recordReactionRepository.existsByRecordIdAndUserIdAndType(recordId, currentUser.getId(), type)) {
            throw new CustomException(ErrorCode.DUPLICATE_REACTION);
        }

        String value = String.format(CacheConstants.REACTION_QUEUE_FORMAT, currentUser.getId(), recordId, type.name());
        redisTemplate.opsForList().rightPush(CacheConstants.REACTION_QUEUE, value);

        reactionRankingService.incrementRankingScore(recordId);

        return ReactToRecordResDto.of(type, recordId);
    }
}
