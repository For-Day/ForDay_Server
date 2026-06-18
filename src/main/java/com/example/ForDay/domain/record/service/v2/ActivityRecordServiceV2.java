package com.example.ForDay.domain.record.service.v2;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.notification.repository.NotificationRepository;
import com.example.ForDay.domain.notification.service.NotificationService;
import com.example.ForDay.domain.reaction.service.ReactionRedisLockService;
import com.example.ForDay.domain.record.service.RecordCacheService;
import com.example.ForDay.domain.record.service.StickerInfoCacheService;
import com.example.ForDay.domain.record.dto.ReactionSummary;
import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.dto.ReportActivityRecordDto;
import com.example.ForDay.domain.record.dto.request.ActivityRecordReqDtoV2;
import com.example.ForDay.domain.record.dto.request.RecordSearchConditionReqDto;
import com.example.ForDay.domain.record.dto.request.UpdateActivityRecordReqDtoV2;
import com.example.ForDay.domain.record.dto.response.*;
import com.example.ForDay.domain.reaction.repository.ActivityRecordReactionRepository;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.RecordImage;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordScrapRepository;
import com.example.ForDay.domain.record.repository.RecordImageRepository;
import com.example.ForDay.domain.reaction.service.ReactionRankingService;
import com.example.ForDay.domain.record.type.ContextType;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.domain.record.utils.ActivityRecordUtil;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.util.S3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityRecordServiceV2 {
    private final ActivityRecordRepository activityRecordRepository;
    private final UserUtil userUtil;
    private final ActivityRecordReactionRepository recordReactionRepository;
    private final ActivityRecordScrapRepository activityRecordScrapRepository;
    private final S3Util s3Util;
    private final ReactionRankingService reactionRankingService;
    private final ActivityRecordUtil activityRecordUtil;
    private final NotificationService notificationService;
    private final ReactionRedisLockService reactionRedisLockService;
    private final HobbyRepository hobbyRepository;
    private final ActivityRepository activityRepository;
    private final RecordImageRepository recordImageRepository;
    private final NotificationRepository notificationRepository;
    private final StickerInfoCacheService stickerInfoCacheService;
    private final RecordCacheService recordCacheService;

    // 위, 아래 스와이프 적용 버전
    @Transactional
    public GetRecordDetailResDtoV2 getRecordDetailV2(Long recordId, RecordSearchConditionReqDto condition, CustomUserDetails user, List<Long> hobbyIds, Long notificationId) {
        User currentUser = userUtil.getCurrentUser(user);

        validateAccess(condition, currentUser);
        validateCondition(condition, hobbyIds);

        RecordDetailQueryDto detail = activityRecordRepository.findDetailDtoById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        boolean isRecordOwner = activityRecordUtil.isRecordOwner(currentUser.getId(), detail.writerId());
        if(isRecordOwner) notificationService.markAsReadIfUnread(notificationId);

        if (detail.recordDeleted()) throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);

        if (!isRecordOwner) {
            activityRecordUtil.validateAccess(currentUser.getId(), detail.writerId(), detail.writerDeleted(), detail.visibility());
        }

        List<ReactionSummary> summaries = recordReactionRepository.findOptimizedSummaries(recordId, currentUser.getId());

        Long prevId = activityRecordRepository.findPrevRecordId(recordId, detail.createdAt(), condition, currentUser.getId(), hobbyIds);
        Long nextId = activityRecordRepository.findNextRecordId(recordId, detail.createdAt(), condition, currentUser.getId(), hobbyIds);

        return GetRecordDetailResDtoV2.of(detail, isRecordOwner, isScraped(detail, currentUser), prevId, nextId, summaries, s3Util.toProfileMainResizedUrl(detail.writerProfileImageUrl()), currentUser.getId());
    }

    @Transactional
    public ReactToRecordResDto reactToRecordWithRedis(Long recordId, RecordReactionType reactionType, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        ReportActivityRecordDto record = activityRecordUtil.getValidRecord(recordId);

        if (!activityRecordUtil.isRecordOwner(currentUser.getId(), record.getWriterId())) {
            activityRecordUtil.validateAccess(currentUser.getId(), record.getWriterId(), record.isWriterDeleted(), record.getVisibility());
        }
        reactionRedisLockService.createReactionWithRedis(currentUser.getId(), recordId, reactionType);

        // 랭킹 점수는 즉시 반영
        reactionRankingService.incrementRankingScore(recordId);

        return new ReactToRecordResDto("반응이 정상적으로 등록되었습니다.", reactionType, recordId);
    }

    @Transactional
    public ActivityRecordResDto recordActivity(ActivityRecordReqDtoV2 reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        Hobby hobby = hobbyRepository.findByIdAndUserId(reqDto.getHobbyId(), currentUser.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.HOBBY_NOT_FOUND));
        hobby.validateCanRecord();

        Activity activity = resolveActivity(reqDto, currentUser, hobby);
        activity.record();

        ActivityRecord record = ActivityRecord.ofV2(hobby, activity, currentUser, reqDto);
        activityRecordRepository.save(record);

        List<RecordImage> images = buildRecordImages(record, reqDto.getImages());
        recordImageRepository.saveAll(images);

        return ActivityRecordResDto.from(record, images);
    }

    private boolean isScraped(RecordDetailQueryDto detail, User currentUser) {
        return activityRecordScrapRepository.existsByScrap(detail.recordId(), currentUser.getId());
    }

    private void validateCondition(RecordSearchConditionReqDto condition, List<Long> hobbyIds) {
        if (condition.context() == ContextType.STORY_HOBBY && hobbyIds.isEmpty()) {
            throw new CustomException(ErrorCode.HOBBY_ID_REQUIRED);
        }
    }

    private static void validateAccess(RecordSearchConditionReqDto condition, User currentUser) {
        if(currentUser.getRole().equals(Role.GUEST)) {
            if (isStoryContext(condition)) {
                throw new CustomException(ErrorCode.ACCESS_DENIED_FOR_GUEST);
            }
        }
    }

    private static boolean isStoryContext(RecordSearchConditionReqDto condition) {
        return condition.context() == ContextType.STORY_ALL || condition.context() == ContextType.STORY_HOBBY;
    }

    private Activity resolveActivity(ActivityRecordReqDtoV2 reqDto, User currentUser, Hobby hobby) {
        if (reqDto.getActivityId() != null) {
            return activityRepository.findByIdAndUserId(reqDto.getActivityId(), currentUser.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
        }
        return activityRepository.save(Activity.createActivity(currentUser, hobby, reqDto.getActivityContent(), false));
    }

    @Transactional
    public UpdateActivityRecordResDtoV2 updateActivityRecord(Long recordId, UpdateActivityRecordReqDtoV2 reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        ActivityRecord record = activityRecordUtil.getRecordByUserId(recordId, currentUser);

        Activity activity = resolveActivityForUpdate(reqDto.getActivityId(), currentUser, record);

        List<RecordImage> oldImages = recordImageRepository.findAllByActivityRecordIdOrderByImageOrderAsc(recordId);
        oldImages.forEach(img -> s3Util.registerS3DeletionAfterCommit(img.getImageUrl()));
        recordImageRepository.deleteAll(oldImages);

        record.updateRecordV2(activity, reqDto);

        List<RecordImage> newImages = buildRecordImages(record, reqDto.getImages());
        recordImageRepository.saveAll(newImages);

        String newThumbnailUrl = newImages.isEmpty() ? null : newImages.get(0).getImageUrl();
        notificationRepository.updateImageUrlByRecordId(recordId, newThumbnailUrl);

        stickerInfoCacheService.evictRecordCache(record.getHobby().getId(), currentUser.getId());
        recordCacheService.evictRecordCache(record.getId());

        return UpdateActivityRecordResDtoV2.builder()
                .message("활동 기록이 정상적으로 수정되었습니다.")
                .activityRecordId(record.getId())
                .activityId(activity.getId())
                .activityContent(activity.getContent())
                .sticker(record.getSticker())
                .memo(record.getMemo())
                .visibility(record.getVisibility())
                .images(newImages.stream().map(ActivityRecordResDto.ImageInfo::from).toList())
                .build();
    }

    private Activity resolveActivityForUpdate(Long activityId, User currentUser, ActivityRecord record) {
        if (activityId == null) {
            return record.getActivity();
        }
        return activityRepository.findByIdAndUserId(activityId, currentUser.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
    }

    private List<RecordImage> buildRecordImages(ActivityRecord record, List<ActivityRecordReqDtoV2.ActivityImageReqDto> images) {
        if (images == null || images.isEmpty()) return Collections.emptyList();
        return images.stream()
                .map(img -> RecordImage.of(record, img, img.getImageOrder() == 1))
                .toList();
    }
}