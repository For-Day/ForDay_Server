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
import com.example.ForDay.global.util.ImageUrlConverter;
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
    private final ImageUrlConverter imageUrlConverter;
    private final ActivityRecordRepository activityRecordRepository;
    private final ActivityRecordReactionRepository recordReactionRepository;
    private final ReactionRankingService reactionRankingService;
    private final UserRepository userRepository;
    private final ActivityRecordReactionCountRepository recordReactionCountRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final NotificationService notificationService;

    @Transactional
    public ReactionSummaryResDto getReactionSummary(Long recordId, int size, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        ActivityRecord record = activityRecordUtil.getRecord(recordId);
        ReactionSummaryResDto.ReactionCountDto reactionCountDto = getReactionCountSummary(record.getId());

        Map<String, ReactionSummaryResDto.ReactionSliceDto> tabs = activityRecordReactionRepository.getReactionSummary(recordId, size, currentUser.getId());
        processReactionProfileUrls(tabs);
        tabs.values().forEach(slice -> processWithdrawnUsers(slice.getUsers()));

        boolean isRecordOwner = activityRecordUtil.isRecordOwner(currentUser.getId(), record.getUser().getId());

        if (isRecordOwner) {
            recordReactionRepository.markAsReadByRecordId(recordId);
        }

        return ReactionSummaryResDto.of(record.getId(), reactionCountDto, tabs);
    }

    @Transactional
    public ReactionTabScrollResDto getReactionTabScroll(Long recordId, RecordReactionType type, Long lastReactionId, int size, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        ActivityRecord record = activityRecordUtil.getRecord(recordId);

        ReactionTabScrollResDto response = activityRecordReactionRepository.getReactionTabScroll(record.getId(), type, lastReactionId, size, currentUser.getId());

        if (response != null && response.getTabs() != null) {
            response.getTabs().values().forEach(slice -> processWithdrawnUsers(slice.getUsers()));
        }

        return response;
    }

    @Transactional
    public GetRecordReactionUsersResDto getRecordReactionUsers(Long recordId, RecordReactionType type, CustomUserDetails user, String lastUserId, Integer size) {
        RecordDetailQueryDto recordDetail = activityRecordRepository.findDetailDtoById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        if (recordDetail.recordDeleted()) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }

        String currentUserId = userUtil.getCurrentUser(user).getId();
        activityRecordUtil.validateAccess(currentUserId, recordDetail.writerId(), recordDetail.writerDeleted(), recordDetail.visibility());

        boolean isRecordOwner = activityRecordUtil.isRecordOwner(currentUserId, recordDetail.writerId());
        List<GetRecordReactionUsersResDto.ReactionUserInfo> reactionUsers = recordReactionRepository.findReactionUsersDtoByType(recordId, type, lastUserId, size, isRecordOwner);

        if (isRecordOwner) {
            recordReactionRepository.markAsReadByRecordIdAndType(recordId, type);
        }

        return GetRecordReactionUsersResDto.of(type, reactionUsers, size);
    }

    // 푸시 알림 비동기 처리시
    @Transactional
    public ReactToRecordResDto reactToRecord(Long recordId, RecordReactionType type, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        ReportActivityRecordDto record = activityRecordUtil.getValidRecord(recordId);
        if(!isRecordOwner(currentUser, record)) activityRecordUtil.validateAccess(currentUser.getId(), record.getWriterId(), record.isWriterDeleted(), record.getVisibility());
        validateDuplicateReaction(recordId, currentUser.getId(), type);

        ActivityRecordReaction reaction = ActivityRecordReaction.of(activityRecordRepository.getReferenceById(recordId), userRepository.getReferenceById(currentUser.getId()), type);
        recordReactionRepository.save(reaction);

        int result = recordReactionCountRepository.increaseCount(recordId, type.toString());
        if (result == 0) {
            recordReactionCountRepository.save(ActivityRecordReactionCount.init(recordId, type));
        }
        reactionRankingService.incrementRankingScore(record.getRecordId());

        if(!isRecordOwner(currentUser, record)) {
            notificationService.processReactionNotification(currentUser, userRepository.getReferenceById(record.getWriterId()), type, record.getRecordId(), record.getImageUrl());
        }

        return ReactToRecordResDto.of(type, recordId);
    }

    // 푸시 알림 동기 처리시
    @Transactional
    public ReactToRecordResDto testReactToRecord(Long recordId, RecordReactionType type, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        ReportActivityRecordDto record = activityRecordUtil.getValidRecord(recordId);
        if(!isRecordOwner(currentUser, record)) activityRecordUtil.validateAccess(currentUser.getId(), record.getWriterId(), record.isWriterDeleted(), record.getVisibility());
        validateDuplicateReaction(recordId, currentUser.getId(), type);

        ActivityRecordReaction reaction = ActivityRecordReaction.of(activityRecordRepository.getReferenceById(recordId), userRepository.getReferenceById(currentUser.getId()), type);
        recordReactionRepository.save(reaction);

        int result = recordReactionCountRepository.increaseCount(recordId, type.toString());
        if (result == 0) {
            recordReactionCountRepository.save(ActivityRecordReactionCount.init(recordId, type));
        }
        reactionRankingService.incrementRankingScore(record.getRecordId());

        if(!isRecordOwner(currentUser, record)) {
            // 여기만 수정
            notificationService.testProcessReactionNotification(currentUser, userRepository.getReferenceById(record.getWriterId()), type, record.getRecordId(), record.getImageUrl());
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
        reactionRankingService.decrementRankingScore(recordId);

        log.info("[cancelReactToRecord] 리액션 취소 완료 - RecordId: {}, UserId: {}", recordId, userId);
        return CancelReactToRecordResDto.of(type, recordId);
    }

    private void processReactionProfileUrls(Map<String, ReactionSummaryResDto.ReactionSliceDto> tabs) {
        tabs.values().forEach(sliceDto -> {
            if (sliceDto != null && sliceDto.getUsers() != null) {
                sliceDto.getUsers().forEach(userDto -> {
                    userDto.setProfileImageUrl(
                            imageUrlConverter.toProfileListResizedUrl(userDto.getProfileImageUrl())
                    );
                });
            }
        });
    }

    private ReactionSummaryResDto.ReactionCountDto getReactionCountSummary(Long recordId) {
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

    private void processWithdrawnUsers(List<ReactionSummaryResDto.ReactionUserDto> users) {
        if (users == null) return;
        users.forEach(userDto -> {
            if (userDto.getNickname() != null && userDto.getNickname().startsWith("WITHDRAWN")) {
                userDto.setNickname("탈퇴한 사용자");
            }
        });
    }
}
