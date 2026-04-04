package com.example.ForDay.domain.activity.service;

import com.example.ForDay.domain.activity.dto.ActivityRecordCollectInfo;
import com.example.ForDay.domain.activity.dto.request.UpdateActivityReqDto;
import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityBulkRepository;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.activity.utils.ActivityUtil;
import com.example.ForDay.domain.friend.repository.FriendRelationRepository;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.hobby.dto.request.AddActivityReqDto;
import com.example.ForDay.domain.hobby.dto.request.RecordActivityReqDto;
import com.example.ForDay.domain.hobby.dto.response.AddActivityResDto;
import com.example.ForDay.domain.hobby.dto.response.CollectActivityResDto;
import com.example.ForDay.domain.hobby.dto.response.RecordActivityResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.service.HobbyCardService;
import com.example.ForDay.domain.hobby.utils.HobbyUtil;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.service.StickerInfoCacheService;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.ai.service.TodayRecordRedisService;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.s3.util.S3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

import static com.example.ForDay.global.common.response.message.ActivitySuccessMessage.ACTIVITY_DELETE_SUCCESS;
import static com.example.ForDay.global.common.response.message.ActivitySuccessMessage.ACTIVITY_UPDATE_SUCCESS;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {
    private static final Integer STICKER_COMPLETE_COUNT = 66;

    private final UserUtil userUtil;
    private final ActivityRepository activityRepository;
    private final ActivityRecordRepository activityRecordRepository;
    private final TodayRecordRedisService todayRecordRedisService;
    private final FriendRelationRepository friendRelationRepository;
    private final HobbyUtil hobbyUtil;
    private final HobbyCardService hobbyCardService;
    private final ActivityUtil activityUtil;
    private final S3Util s3Util;
    private final ActivityBulkRepository activityBulkRepository;
    private final StickerInfoCacheService recordRedisService;
    private final ActivityCacheService activityCacheService;

    @Transactional
    public RecordActivityResDto recordActivity(Long activityId, RecordActivityReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[RecordActivity] 시작 - UserId: {}, ActivityId: {}", currentUser.getId(), activityId);

        Activity activity = activityRepository.findByIdAndUserIdWithHobby(activityId, currentUser.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
        Hobby hobby = activity.getHobby();

        hobby.validateCanRecord();
        todayRecordRedisService.validateNotRecordedToday(currentUser.getId(), hobby.getId());
        s3Util.validateS3Image(reqDto.getImageUrl());

        ActivityRecord activityRecord = ActivityRecord.of(hobby, activity, currentUser, reqDto);
        activity.record();
        currentUser.obtainSticker();
        activityRecordRepository.save(activityRecord);

        log.info("[RecordActivity] 기록 저장 성공 - RecordId: {}, 현재 스티커 수: {}", activityRecord.getId(), hobby.getCurrentStickerNum());

        if (hobby.isStickerFull()) {
            log.info("[RecordActivity] 취미 완주 달성! 취미 카드 생성을 시작합니다. HobbyId: {}", hobby.getId());
            createHobbyCard(hobby, currentUser);
        }
        todayRecordRedisService.markAsRecorded(currentUser.getId(), hobby.getId());
        recordRedisService.evictRecordCache(hobby.getId(), currentUser.getId());

        return RecordActivityResDto.of(hobby, activityRecord, activity, reqDto.getSticker(), isCheckStickerFull(hobby));
    }

    @Transactional
    public RecordActivityResDto testRecordActivity(Long activityId, RecordActivityReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        Activity activity = activityRepository.findByIdAndUserIdWithHobby(activityId, currentUser.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
        Hobby hobby = activity.getHobby();

        if (isCheckStickerFull(hobby)) throw new CustomException(ErrorCode.STICKER_COMPLETION_REACHED);
        hobby.validateInProgress();
        s3Util.validateS3Image(reqDto.getImageUrl());

        ActivityRecord activityRecord = ActivityRecord.of(hobby, activity, currentUser, reqDto);

        activity.record();
        currentUser.obtainSticker();
        activityRecordRepository.save(activityRecord);

        if (Objects.equals(hobby.getCurrentStickerNum(), STICKER_COMPLETE_COUNT)) {
            createHobbyCard(hobby, currentUser);
        }
        recordRedisService.evictRecordCache(hobby.getId(), currentUser.getId());

        return RecordActivityResDto.of(hobby, activityRecord, activity, reqDto.getSticker(), isCheckStickerFull(hobby));
    }

    @Transactional
    public MessageResDto updateActivity(Long activityId, UpdateActivityReqDto reqDto, CustomUserDetails user) {
        log.info("[ActivityService] 활동 수정 요청 - activityId={}, content={}", activityId, reqDto.getContent());
        User currentUser = userUtil.getCurrentUser(user);
        Activity activity = activityUtil.getActivityByUserId(activityId, currentUser.getId());

        Hobby hobby = activity.getHobby();
        hobby.validateInProgress();

        String beforeContent = activity.getContent();
        activity.updateContent(reqDto.getContent());

        log.info("[ActivityService] 활동 수정 완료 - activityId={}, userId={}, before='{}', after='{}'", activityId, currentUser.getId(), beforeContent, reqDto.getContent());
        activityCacheService.evictCacheAfterCommit(currentUser.getId(), hobby.getId());
        return new MessageResDto(ACTIVITY_UPDATE_SUCCESS);
    }

    @Transactional
    public MessageResDto deleteActivity(Long activityId, CustomUserDetails user) {
        log.info("[ActivityService] 활동 삭제 요청 - activityId={}", activityId);
        User currentUser = userUtil.getCurrentUser(user);
        Activity activity = activityUtil.getActivityByUserId(activityId, currentUser.getId());

        activity.validateDeletable();
        Hobby hobby = activity.getHobby();
        hobby.validateInProgress();
        activityRepository.delete(activity);

        log.info("[ActivityService] 활동 삭제 완료 - activityId={}, userId={}", activityId, currentUser.getId());
        activityCacheService.evictCacheAfterCommit(currentUser.getId(), hobby.getId());
        return new MessageResDto(ACTIVITY_DELETE_SUCCESS);
    }

    @Transactional
    public CollectActivityResDto collectActivity(Long hobbyId, Long activityId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        log.info("[Activity Collect] 시작 - 사용자: {}, 취미ID: {}, 활동ID: {}", currentUser.getId(), hobbyId, activityId);

        Hobby hobby = hobbyUtil.getHobbyByUserId(hobbyId, currentUser);

        ActivityRecordCollectInfo originActivity = activityRepository.getCollectActivityInfo(activityId)
                .orElseThrow(() -> {
                    log.warn("[Activity Collect] 실패 - 원본 활동을 찾을 수 없음. 활동ID: {}", activityId);
                    return new CustomException(ErrorCode.ACTIVITY_NOT_FOUND);
                });

        hobby.validateInProgress();
        validateTargetUser(currentUser.getId(), originActivity);
        log.info("[Activity Collect] 검증 완료 - 활동 소유자ID: {}", originActivity.getUserId());

        Activity newActivity = activityRepository.save(Activity.createActivity(currentUser, hobby, originActivity.getContent(), false));

        log.info("[Activity Collect] 완료 - 생성된 활동ID: {}, 저장된 취미: {}", newActivity.getId(), hobby.getHobbyName());
        return CollectActivityResDto.of(hobby, newActivity);
    }

    @Transactional
    public AddActivityResDto addActivity(Long hobbyId, AddActivityReqDto reqDto, CustomUserDetails user) {
        Hobby hobby = hobbyUtil.getHobby(hobbyId);
        User currentUser = userUtil.getCurrentUser(user);
        hobbyUtil.verifyHobbyOwner(hobby, currentUser);
        hobby.validateInProgress();

        log.info("[AddActivity] 시작 - UserId: {}, HobbyId: {}, 요청 활동 수: {}", currentUser.getId(), hobbyId, reqDto.getActivities().size());

        List<Activity> activities = reqDto.getActivities().stream()
                .map(activity -> Activity.createActivity(currentUser, hobby, activity.getContent(), activity.isAiRecommended()))
                .toList();

        activityBulkRepository.bulkInsertActivities(activities);
        log.info("[AddActivity] 성공 - 저장된 활동 수: {}", activities.size());
        activityCacheService.evictCacheAfterCommit(currentUser.getId(), hobby.getId());

        return AddActivityResDto.of(activities.size());
    }
    private void createHobbyCard(Hobby hobby, User currentUser) {
        log.info("[HobbyCard] 생성 프로세스 시작 - 사용자: {}, 취미: {}", currentUser.getId(), hobby.getId());

        try {
            hobbyCardService.createHobbyCard(currentUser, hobby);
            currentUser.obtainHobbyCard();

        } catch (Exception e) {
            todayRecordRedisService.deleteTodayRecordKey(currentUser.getId(), hobby.getId());
            log.error("[AI-HOBBY-CARD][ERROR] FastAPI 호출 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    private static boolean isCheckStickerFull(Hobby hobby) {
        if (hobby.getCurrentStickerNum() == null || hobby.getGoalDays() == null) {
            return false;
        }
        return Objects.equals(hobby.getCurrentStickerNum().intValue(), STICKER_COMPLETE_COUNT)
                && Objects.equals(hobby.getGoalDays().intValue(), STICKER_COMPLETE_COUNT);
    }

    private void validateTargetUser(String currentUserId, ActivityRecordCollectInfo target) {
        if (target.isUserDeleted()) {
            throw new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND);
        }

        // 차단 관계 확인 (양방향)
        boolean isBlocked = friendRelationRepository.existsByFriendship(currentUserId, target.getUserId(), FriendRelationStatus.BLOCK)
                || friendRelationRepository.existsByFriendship(target.getUserId(), currentUserId, FriendRelationStatus.BLOCK);

        if (isBlocked) {
            throw new CustomException(ErrorCode.ACTIVITY_NOT_FOUND);
        }
    }
}
