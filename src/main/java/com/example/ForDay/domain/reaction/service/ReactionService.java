package com.example.ForDay.domain.reaction.service;

import com.example.ForDay.domain.notification.service.NotificationService;
import com.example.ForDay.domain.reaction.entity.ActivityRecordReaction;
import com.example.ForDay.domain.reaction.entity.ActivityRecordReactionCount;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionCountRepository;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.dto.ReportActivityRecordDto;
import com.example.ForDay.domain.record.dto.response.*;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.record.utils.ActivityRecordUtil;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.util.S3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReactionService {
    private static final String REACTION_DONE_KEY_FORMAT = "reaction:done:%d:%s";

    private final ActivityRecordUtil activityRecordUtil;
    private final ActivityRecordReactionRepository activityRecordReactionRepository;
    private final ActivityRecordReactionCountRepository activityRecordReactionCountRepository;
    private final UserUtil userUtil;
    private final S3Util s3Util;
    private final ActivityRecordRepository activityRecordRepository;
    private final ActivityRecordReactionRepository recordReactionRepository;
    private final ReactionRankingService reactionRankingService;
    private final UserRepository userRepository;
    private final ActivityRecordReactionCountRepository recordReactionCountRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public ReactionSummaryResDto getReactionSummary(Long recordId, int size, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        ActivityRecord record = activityRecordUtil.getRecord(recordId); // 조회하고자하는 기록 엔티티 조회

        // 감정 갯수 요약 조회 (무한 스크롤 상관없이 전체 조회해야함) -> 이걸 별도의 ReactionCount 테이블 만들어서 관리 그래서 반응 남길 때 lock 거는 로직 필요
        ReactionSummaryResDto.ReactionCountDto reactionCountDto = getReactionCountSummary(record.getId());

        // size에 따른 전체 유저 목록 조회
        Map<String, ReactionSummaryResDto.ReactionSliceDto> tabs = activityRecordReactionRepository.getReactionSummary(recordId, size, currentUser.getId());
        processReactionProfileUrls(tabs);

        return ReactionSummaryResDto.of(record.getId(), reactionCountDto, tabs);
    }

    @Transactional(readOnly = true)
    public ReactionTabScrollResDto getReactionTabScroll(Long recordId, RecordReactionType type, Long lastReactionId, int size, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        ActivityRecord record = activityRecordUtil.getRecord(recordId);

        return activityRecordReactionRepository.getReactionTabScroll(record.getId(), type, lastReactionId, size, currentUser.getId());
    }

    @Transactional
    public GetRecordReactionUsersResDto getRecordReactionUsers(Long recordId, RecordReactionType type, CustomUserDetails user, String lastUserId, Integer size) {
        // 엔티티 전체 대신 권한 확인용 DTO만 조회 (Fetch Join 제거 효과)
        RecordDetailQueryDto recordDetail = activityRecordRepository.findDetailDtoById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        if (recordDetail.recordDeleted()) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }

        String currentUserId = userUtil.getCurrentUser(user).getId();
        activityRecordUtil.validateAccess(currentUserId, recordDetail.writerId(), recordDetail.writerDeleted(), recordDetail.visibility());

        boolean isRecordOwner = activityRecordUtil.isRecordOwner(currentUserId, recordDetail.writerId());
        // 리포지토리에서 DTO(ReactionUserInfo)로 직접 조회하여 N+1 및 오버페칭 방지
        List<GetRecordReactionUsersResDto.ReactionUserInfo> reactionUsers = recordReactionRepository.findReactionUsersDtoByType(recordId, type, lastUserId, size, isRecordOwner);


        // 게시글 주인인 경우에만 벌크 업데이트 실행
        if (isRecordOwner) {
            recordReactionRepository.markAsReadByRecordIdAndType(recordId, type);
        }

        return GetRecordReactionUsersResDto.of(type, reactionUsers, size);
    }

    @Transactional
    public ReactToRecordResDto reactToRecord(Long recordId, RecordReactionType type, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        ReportActivityRecordDto record = activityRecordUtil.getValidRecord(recordId);
        if(!isRecordOwner(currentUser, record)) activityRecordUtil.validateAccess(currentUser.getId(), record.getWriterId(), record.isWriterDeleted(), record.getVisibility());
        validateDuplicateReaction(recordId, currentUser.getId(), type);

        ActivityRecordReaction reaction = ActivityRecordReaction.of(activityRecordRepository.getReferenceById(recordId), userRepository.getReferenceById(currentUser.getId()), type);
        recordReactionRepository.save(reaction);

        // 반응 수 증가
        int result = recordReactionCountRepository.increaseCount(recordId, type.toString());
        if (result == 0) {
            recordReactionCountRepository.save(ActivityRecordReactionCount.init(recordId, type));
        }
        // 리액션 증가에 따른 랭킹 점수 업데이트
        reactionRankingService.incrementRankingScore(record.getRecordId());

        if(!isRecordOwner(currentUser, record)) {
            notificationService.processReactionNotification(currentUser, userRepository.getReferenceById(record.getWriterId()), type, record.getRecordId(), record.getImageUrl());
        }

        return ReactToRecordResDto.of(type, recordId);
    }

    @Transactional
    public CancelReactToRecordResDto cancelReactToRecord(Long recordId, RecordReactionType type, CustomUserDetails user) {
        String userId = user.getUserId();

        int deletedCount = recordReactionRepository.deleteByRecordIdAndUserIdAndType(recordId, userId, type);
        if (deletedCount == 0) {
            throw new CustomException(ErrorCode.REACTION_NOT_FOUND);
        }

        recordReactionCountRepository.decreaseCount(recordId, type.name());

        String key = REACTION_DONE_KEY_FORMAT.formatted(recordId, userId);
        redisTemplate.opsForSet().remove(key, type.name());

        // Redis 점수 차감
        reactionRankingService.decrementRankingScore(recordId);

        log.info("[cancelReactToRecord] 리액션 취소 완료 - RecordId: {}, UserId: {}", recordId, userId);
        return CancelReactToRecordResDto.of(type, recordId);
    }

    private void processReactionProfileUrls(Map<String, ReactionSummaryResDto.ReactionSliceDto> tabs) {
        tabs.values().forEach(sliceDto -> {
            if (sliceDto != null && sliceDto.getUsers() != null) {
                sliceDto.getUsers().forEach(userDto -> {
                    userDto.setProfileImageUrl(
                            s3Util.toProfileListResizedUrl(userDto.getProfileImageUrl())
                    );
                });
            }
        });
    }

    private ReactionSummaryResDto.ReactionCountDto getReactionCountSummary(Long recordId) {
        // recordId에 해당하는 ActivityRecordReactionCount 조회
        return activityRecordReactionCountRepository.findById(recordId)
                .map(ReactionSummaryResDto::from)
                .orElseGet(ReactionSummaryResDto::empty);
    }

    private static boolean isRecordOwner(User currentUser, ReportActivityRecordDto record) {
        return currentUser.getId().equals(record.getWriterId());
    }

    private void validateDuplicateReaction(Long recordId, String userId, RecordReactionType type) {
        if (recordReactionRepository.existsByRecordIdAndUserIdAndType(recordId, userId, type)) {
            throw new CustomException(ErrorCode.DUPLICATE_REACTION);
        }
    }
}
