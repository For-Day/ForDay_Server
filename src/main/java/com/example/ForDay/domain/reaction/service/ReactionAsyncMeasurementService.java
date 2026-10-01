package com.example.ForDay.domain.reaction.service;

import com.example.ForDay.domain.record.dto.ReportActivityRecordDto;
import com.example.ForDay.domain.record.dto.response.ReactToRecordResDto;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.record.utils.ActivityRecordUtil;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * #375 4단계 재측정(멘토 피드백 반영판)의 2단계 - Spring {@code @Async}만 적용, Redis 큐는
 * 아직 없음. {@code measure} 프로파일에서만 빈으로 등록되어 프로덕션에는 존재하지 않는다.
 *
 * <p>중복확인·랭킹 갱신은 v1/{@link QueueOnlyReactionMeasurementService}와 동일하게 응답
 * 스레드에서 동기로 처리한다 - 이 단계가 격리해서 보려는 변수는 "반응 저장 + 카운트 upsert를
 * 비동기로 미루면 무슨 일이 생기는가" 하나뿐이다. 실제 저장은
 * {@link ReactionAsyncDispatcher#dispatch}로 위임한다.
 */
@Service
@Profile("measure")
@RequiredArgsConstructor
public class ReactionAsyncMeasurementService {
    private final UserUtil userUtil;
    private final ActivityRecordUtil activityRecordUtil;
    private final ActivityRecordReactionRepository recordReactionRepository;
    private final ReactionRankingService reactionRankingService;
    private final ReactionAsyncDispatcher reactionAsyncDispatcher;

    public ReactToRecordResDto reactToRecordAsync(Long recordId, RecordReactionType type, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        ReportActivityRecordDto record = activityRecordUtil.getValidRecord(recordId);

        if (!activityRecordUtil.isRecordOwner(currentUser.getId(), record.getWriterId())) {
            activityRecordUtil.validateAccess(currentUser.getId(), record.getWriterId(), record.isWriterDeleted(), record.getVisibility());
        }

        if (recordReactionRepository.existsByRecordIdAndUserIdAndType(recordId, currentUser.getId(), type)) {
            throw new CustomException(ErrorCode.DUPLICATE_REACTION);
        }

        reactionRankingService.incrementRankingScore(recordId);
        reactionAsyncDispatcher.dispatch(recordId, type, currentUser.getId());

        return ReactToRecordResDto.of(type, recordId);
    }
}
