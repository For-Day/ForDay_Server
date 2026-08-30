package com.example.ForDay.domain.hobby.service.v1;

import com.example.ForDay.domain.activity.entity.ActivityRecommendItem;
import com.example.ForDay.domain.activity.repository.ActivityRecommendItemRepository;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.activity.service.ActivityCacheService;
import com.example.ForDay.domain.hobby.dto.AiInsightResult;
import com.example.ForDay.domain.hobby.dto.CoverChangeResult;
import com.example.ForDay.domain.hobby.dto.StickerContext;
import com.example.ForDay.domain.hobby.dto.request.*;
import com.example.ForDay.domain.hobby.dto.response.*;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.service.HobbyAiInsightService;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.hobby.type.StickerCover;
import com.example.ForDay.domain.hobby.utils.HobbyUtil;
import com.example.ForDay.domain.hobby.validator.HobbyValidator;
import com.example.ForDay.domain.notification.service.NotificationService;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.service.StickerInfoCacheService;
import com.example.ForDay.domain.record.service.TodayRecordRedisService;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.ai.service.AiActivityRecommendService;
import com.example.ForDay.global.ai.service.AiCallCountService;
import com.example.ForDay.domain.hobby.service.HobbyAiSummaryService;
import com.example.ForDay.global.common.constants.AiMessageConstants;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.common.response.message.HobbySuccessCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.domain.hobby.port.CoverGeneratorPort;
import com.example.ForDay.global.util.ImageUrlConverter;
import com.example.ForDay.global.port.ImageLifecyclePort;
import com.example.ForDay.global.port.ImageUrlPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

import static com.example.ForDay.global.common.constants.FileStorageConstants.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HobbyService {
    private static final Integer DEFAULT_GOAL_DAYS = 66;
    private static final Integer STICKER_COMPLETE_COUNT = 66;

    @Value("${ai.max-call-limit}")
    private int maxCallLimit;

    private final HobbyRepository hobbyRepository;
    private final UserUtil userUtil;
    private final ActivityRepository activityRepository;
    private final AiCallCountService aiCallCountService;
    private final ActivityRecordRepository activityRecordRepository;
    private final TodayRecordRedisService todayRecordRedisService;
    private final HobbyAiSummaryService hobbyAiSummaryService;
    private final CoverGeneratorPort coverGeneratorPort;
    private final ImageUrlConverter imageUrlConverter;
    private final ImageLifecyclePort imageLifecyclePort;
    private final ImageUrlPort imageUrlPort;
    private final ActivityRecommendItemRepository activityRecommendItemRepository;
    private final HobbyAiInsightService hobbyAiInsightService;
    private final AiActivityRecommendService aiActivityRecommendService;
    private final HobbyUtil hobbyUtil;
    private final StickerInfoCacheService stickerInfoCacheService;
    private final ActivityCacheService activityCacheService;
    private final NotificationService notificationService;
    private final HobbyValidator hobbyValidator;

    @Transactional
    public HobbyCreateResDto hobbyCreate(HobbyCreateReqDto reqDto, CustomUserDetails userDetails) {
        User currentUser = userUtil.getCurrentUser(userDetails);
        log.info("[HobbyCreate] Start - userId={}, hobbyId={}, hobbyName={}", currentUser.getId(), reqDto.getHobbyInfoId(), reqDto.getHobbyName());

        if (currentUser.isOnboardingCompleted() && !currentUser.isNicknameSet()) {
            throw new CustomException(ErrorCode.DUPLICATE_HOBBY_REQUEST);
        }

        hobbyValidator.validateMaxInProgressHobbies(currentUser);
        hobbyValidator.validateDuplicateHobby(reqDto, currentUser);
        Hobby savedHobby = hobbyRepository.save(Hobby.createNewHobby(currentUser, reqDto, DEFAULT_GOAL_DAYS));

        if (!currentUser.isOnboardingCompleted()) {
            currentUser.completeOnboarding();
            log.info("[HobbyCreate] User onboarding marked as completed: userId={}", currentUser.getId());
        }

        return HobbyCreateResDto.of(savedHobby.getId());
    }

    @Transactional
    public ActivityAIRecommendResDto activityAiRecommend(Long hobbyId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        Hobby hobby = hobbyValidator.validateHobbyAccess(hobbyId, currentUser);

        int currentCount = aiCallCountService.increaseAndGet(currentUser.getSocialId(), hobbyId);

        try {
            FastAPIRecommendResDto response = aiActivityRecommendService.requestActivityRecommendAI(currentUser, hobby);
            String summary = AiMessageConstants.formatHobbySummary(hobbyAiSummaryService.determine(currentUser, hobby));
            saveRecommendItems(hobby, response);

            return ActivityAIRecommendResDto.of(currentCount, maxCallLimit, summary, response.getActivities());
        } catch (Exception e) {
            aiCallCountService.decrease(currentUser.getSocialId(), hobbyId);
            log.error("[AI-RECOMMEND][ERROR] {}", e.getMessage());
            throw new CustomException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public ActivityAIRecommendResDto testActivityAiRecommend(Long hobbyId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        Hobby hobby = hobbyUtil.getHobby(hobbyId);
        hobbyUtil.verifyHobbyOwner(hobby, currentUser); // hobby의 소유자인지 검증
        hobby.validateInProgress(); // 현재 진행 중인 취미인지 확인

        List<ActivityDto> activityDtos = new ArrayList<>();
        activityDtos.add(ActivityDto
                .builder()
                .activityId(1L)
                .topic("가벼운 덤벨 운동")
                .content("덤벨로 양팔 10회 들어보기")
                .description("부담 없이 가벼운 덤벨을 사용해 운동할 수 있어요.")
                .build());

        activityDtos.add(ActivityDto
                .builder()
                .activityId(2L)
                .topic("간단한 플랭크")
                .content("플랭크 자세로 20초 유지해보기")
                .description("짧은 시간 동안 자세를 유지하면 부담이 적어요.")
                .build());

        activityDtos.add(ActivityDto
                .builder()
                .activityId(3L)
                .topic("가벼운 점프 운동")
                .content("제자리에서 점프 15회 해보기")
                .description("가벼운 점프로 쉽게 시작할 수 있어요.")
                .build());

        return ActivityAIRecommendResDto.builder()
                .message("AI가 취미 활동을 추천했습니다.")
                .aiCallCount(1)
                .aiCallLimit(maxCallLimit)
                .recommendedText("포데이 AI가 알맞은 취미 활동을 추천드려요")
                .activities(activityDtos)
                .build();
    }

    @Transactional(readOnly = true)
    public GetHobbyActivitiesResDto getHobbyActivities(Long hobbyId, CustomUserDetails user, Integer size) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[GetHobbyActivities] 조회 시작 - UserId: {}, HobbyId: {}", currentUser.getId(), hobbyId);

        if (!hobbyRepository.existsByIdAndUserId(hobbyId, currentUser.getId())) {
            throw new CustomException(ErrorCode.NOT_HOBBY_OWNER);
        }

        return activityCacheService.getHobbyActivitiesCached(hobbyId, currentUser.getId(), size);
    }

    @Transactional(readOnly = true)
    public GetHomeHobbyInfoResDto getHomeHobbyInfo(Long hobbyId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[GetHomeHobbyInfo] Dashboard inquiry - UserId: {}, TargetHobbyId: {}",
                currentUser.getId(), hobbyId == null ? "DEFAULT(Latest)" : hobbyId);

        Hobby targetHobby = (hobbyId != null) ? hobbyUtil.getHobby(hobbyId) : hobbyUtil.getLatestInProgressHobby(currentUser);

        if (targetHobby == null) {
            return GetHomeHobbyInfoResDto.ofDefault(currentUser.getNickname());
        }

        GetHomeHobbyInfoResDto response = hobbyRepository.getHomeHobbyInfo(targetHobby.getId(), currentUser);
        if (response == null) {
            log.warn("[GetHomeHobbyInfo] Failed to fetch hobby data - HobbyId: {}", targetHobby.getId());
            return GetHomeHobbyInfoResDto.ofDefault(currentUser.getNickname());
        }

        AiInsightResult aiInsight = hobbyAiInsightService.resolveInsight(currentUser, targetHobby);
        log.info("[GetHomeHobbyInfo] Completion - UserId: {}, Hobby: {}, AI Success: {}", currentUser.getId(), targetHobby.getHobbyName(), !aiInsight.summaryText().isEmpty());

        return GetHomeHobbyInfoResDto.of(notificationService.unreadNotificationExists(currentUser), response, currentUser.getNickname(), aiInsight);
    }

    @Transactional(readOnly = true)
    public MyHobbySettingResDto myHobbySetting(CustomUserDetails user, HobbyStatus hobbyStatus) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[MyHobbySetting] 취미 설정 목록 조회 - UserId: {}", currentUser.getId());

        return hobbyRepository.myHobbySetting(currentUser, hobbyStatus);
    }

    @Transactional(readOnly = true)
    public GetActivityListResDto getActivityList(Long hobbyId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[HobbyService] 활동 목록 조회 시작 - hobbyId={}, userId={}", hobbyId, currentUser.getId());

        if (!hobbyRepository.existsByIdAndUserIdAndStatus(hobbyId, currentUser.getId(), HobbyStatus.IN_PROGRESS)) {
            throw new CustomException(ErrorCode.HOBBY_NOT_FOUND);
        }

        GetActivityListResDto result = activityRepository.getActivityList(hobbyId, currentUser.getId());
        log.info("[HobbyService] 활동 목록 조회 완료 - activityCount={}", result.getActivities().size());
        return result;
    }


    @Transactional
    public MessageResDto updateHobbyTime(Long hobbyId, HobbyTimePayload dto, CustomUserDetails user) {
        log.info("[HobbyService] 취미 시간 수정 요청 - hobbyId={}, minutes={}", hobbyId, dto.getMinutes());

        Hobby hobby = hobbyUtil.checkHobbyUpdateable(hobbyId, user);
        Integer before = hobby.getHobbyTimeMinutes();
        hobby.updateHobbyTimeMinutes(dto.getMinutes());

        log.info("[HobbyService] 취미 시간 수정 완료 - hobbyId={}, userId={}, before={}, after={}", hobbyId, hobby.getUser().getId(), before, dto.getMinutes());
        return new MessageResDto(HobbySuccessCode.UPDATE_HOBBY_TIME_SUCCESS.getMessage());
    }


    @Transactional
    public MessageResDto updateExecutionCount(Long hobbyId, ExecutionCountPayload dto, CustomUserDetails user) {
        log.info("[HobbyService] 취미 실행 횟수 수정 요청 - hobbyId={}, executionCount={}", hobbyId, dto.getExecutionCount());
        Hobby hobby = hobbyUtil.checkHobbyUpdateable(hobbyId, user);
        Integer before = hobby.getExecutionCount();
        hobby.updateExecutionCount(dto.getExecutionCount());

        log.info("[HobbyService] 취미 실행 횟수 수정 완료 - hobbyId={}, userId={}, before={}, after={}", hobbyId, hobby.getUser().getId(), before, dto.getExecutionCount());
        return new MessageResDto(HobbySuccessCode.UPDATE_EXECUTION_COUNT_SUCCESS.getMessage());
    }

    @Transactional
    public MessageResDto updateGoalDays(Long hobbyId, GoalDaysPayload dto, CustomUserDetails user) {
        log.info("[HobbyService] 취미 목표 기간 수정 요청 - hobbyId={}, isDurationSet={}", hobbyId, dto.getIsDurationSet());

        Hobby hobby = hobbyUtil.checkHobbyUpdateable(hobbyId, user);
        Integer before = hobby.getGoalDays();
        Integer after = dto.getIsDurationSet() ? 66 : null;

        hobby.updateGoalDays(after);
        log.info("[HobbyService] 취미 목표 기간 수정 완료 - hobbyId={}, userId={}, before={}, after={}", hobbyId, hobby.getUser().getId(), before, after);
        return new MessageResDto(HobbySuccessCode.UPDATE_GOAL_DAYS_SUCCESS.getMessage());
    }

    @Transactional
    public MessageResDto updateHobbyStatus(Long hobbyId, UpdateHobbyStatusReqDto reqDto, CustomUserDetails user) {
        log.info("[HobbyService] 취미 상태 변경 요청 - hobbyId={}, targetStatus={}", hobbyId, reqDto.getHobbyStatus());

        Hobby hobby = hobbyUtil.getHobby(hobbyId);
        User currentUser = userUtil.getCurrentUser(user);

        hobbyUtil.verifyHobbyOwner(hobby, currentUser);

        HobbyStatus currentStatus = hobby.getStatus();
        HobbyStatus targetStatus = reqDto.getHobbyStatus();

        if (currentStatus == targetStatus) {
            log.info("[HobbyService] 취미 상태 변경 요청 무시 (동일 상태) - hobbyId={}, status={}", hobbyId, currentStatus);
            return new MessageResDto(HobbySuccessCode.ALREADY_HOBBY_STATUS.getMessage());
        }

        switch (targetStatus) {
            case IN_PROGRESS -> {
                long inProgressCount = hobbyRepository.countByStatusAndUser(HobbyStatus.IN_PROGRESS, currentUser);
                if (inProgressCount >= 2) {
                    log.warn("[HobbyService] 진행 중 취미 개수 초과 - userId={}, count={}", currentUser.getId(), inProgressCount);
                    throw new CustomException(ErrorCode.MAX_IN_PROGRESS_HOBBY_EXCEEDED);
                }

                hobby.updateHobbyStatus(HobbyStatus.IN_PROGRESS);
                return new MessageResDto(hobby.getHobbyName() + "취미를 꺼냈어요.");
            }
            case ARCHIVED -> {
                hobby.updateHobbyStatus(HobbyStatus.ARCHIVED);
                return new MessageResDto(hobby.getHobbyName() + "취미가 보관되었어요.");
            }
            default -> {
                log.warn("[HobbyService] 잘못된 취미 상태 요청 - hobbyId={}, status={}", hobbyId, targetStatus);
                throw new CustomException(ErrorCode.INVALID_HOBBY_STATUS);
            }
        }
    }

    @Transactional
    public SetHobbyExtensionResDto setHobbyExtension(Long hobbyId, SetHobbyExtensionReqDto reqDto, CustomUserDetails user) {
        Hobby hobby = hobbyUtil.getHobby(hobbyId);
        User currentUser = userUtil.getCurrentUser(user);
        hobbyUtil.verifyHobbyOwner(hobby, currentUser);

        if (hobby.getGoalDays() == null) {
            throw new CustomException(ErrorCode.HOBBY_PERIOD_NOT_SET);
        }

        if (hobby.getCurrentStickerNum() < STICKER_COMPLETE_COUNT) {
            throw new CustomException(ErrorCode.HOBBY_STICKER_NOT_ENOUGH);
        }

        switch (reqDto.getType()) {
            case CONTINUE -> hobby.setGoalDaysExtension();
            case ARCHIVE -> hobby.setHobbyArchived();
            default -> throw new CustomException(ErrorCode.INVALID_HOBBY_EXTENSION_TYPE);

        }
        return SetHobbyExtensionResDto.of(hobbyId, reqDto.getType());
    }

    @Transactional(readOnly = true)
    public GetStickerInfoResDto getStickerInfo(Long hobbyId, Integer page, Integer size, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        Hobby hobby = resolveHobby(hobbyId, currentUser);
        hobbyValidator.validateHobbyAccess(hobby.getId(), currentUser);

        boolean recordedToday = todayRecordRedisService.hasKey(todayRecordRedisService.createRecordKey(currentUser.getId(), hobby.getId()));

        StickerContext context = StickerContext.of(hobby, recordedToday, page, size);
        List<GetStickerInfoResDto.StickerDto> stickers =  stickerInfoCacheService.getCachedStickers(hobby.getId(), context.getCurrentPage(), context.getSize(), currentUser.getId());

        return GetStickerInfoResDto.of(hobby, context, stickers);
    }

    @Transactional
    public SetHobbyCoverImageResDto setHobbyCoverImage(SetHobbyCoverImageReqDto reqDto, CustomUserDetails user) throws Exception {
        User currentUser = userUtil.getCurrentUser(user);

        CoverChangeResult result;
        if (isDirectUploadCase(reqDto)) {
            result = handleDirectUpload(reqDto, currentUser);
        } else if (isRecordCase(reqDto)) {
            result = handleFromRecord(reqDto, currentUser);
        } else {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return SetHobbyCoverImageResDto.of(result, imageUrlConverter.toCoverMainResizedUrl(result.updatedCoverUrl()));
    }

    @Transactional(readOnly = true)
    public CanCreateHobbyResDto canCreateHobby(String name, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        if (hobbyRepository.existsByHobbyNameAndUserId(name, currentUser.getId())) {
            return CanCreateHobbyResDto.canNotCreate();
        }
        return CanCreateHobbyResDto.canCreate();
    }

    @Transactional(readOnly = true)
    public ReCheckHobbyInfoResDto reCheckHobbyInfo(CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        List<ReCheckHobbyInfoResDto.HobbyInfoDto> hobbyInfoDtos = hobbyRepository.reCheckHobbyInfo(currentUser.getId());
        return new ReCheckHobbyInfoResDto(hobbyInfoDtos);
    }

    @Transactional
    public UpdateHobbyResDto updateHobby(Long hobbyId, UpdateHobbyReqDto reqDto, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        boolean isNicknameSet = StringUtils.hasText(currentUser.getNickname());
        boolean onboardingCompleted = currentUser.isOnboardingCompleted();

        if (!(onboardingCompleted && !isNicknameSet)) {
            throw new CustomException(ErrorCode.INVALID_HOBBY_STATUS);
        }

        Hobby hobby = hobbyUtil.getHobbyByUserId(hobbyId, currentUser);
        hobby.updateHobby(reqDto, reqDto.isDurationSet() ? DEFAULT_GOAL_DAYS : null);

        return UpdateHobbyResDto.from(hobby);
    }

    @Transactional(readOnly = true)
    public GetHobbyListByChipResDto getHobbyListByChip(HobbyStatus status, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        List<Hobby> hobbies = getHobbiesByStatus(currentUser.getId(), status);

        List<GetHobbyListByChipResDto.HobbyInfoByChip> hobbyInfos =
                hobbies.stream()
                        .map(hobby -> toHobbyInfo(currentUser.getId(), hobby))
                        .toList();
        return new GetHobbyListByChipResDto(hobbyInfos);
    }

    @Transactional
    public DeleteHobbyResDto deleteHobby(Long hobbyId, User user) {
        Hobby hobby = hobbyUtil.getHobbyByUserId(hobbyId, user);

        if (hobby.isProgressed()) {
            long progressHobbyCount = hobbyRepository.countByUserAndStatus(user, HobbyStatus.IN_PROGRESS);

            if (progressHobbyCount <= 1) {
                throw new CustomException(ErrorCode.MINIMUM_PROGRESS_HOBBY_REQUIRED);
            }
        }

        hobby.deleteHobby();
        hobbyRepository.saveAndFlush(hobby);

        activityRecordRepository.bulkDeleteByHobby(hobby);
        return DeleteHobbyResDto.of(hobby.getId());
    }

    private List<Hobby> getHobbiesByStatus(String userId, HobbyStatus status) {
        if (status == HobbyStatus.ALL) {
            return hobbyRepository.findAllByUserIdOrderByIdDesc(userId);
        }
        return hobbyRepository.findAllByUserIdAndStatusOrderByIdDesc(userId, status);
    }

    private GetHobbyListByChipResDto.HobbyInfoByChip toHobbyInfo(String userId, Hobby hobby) {
        boolean todayRecorded = isTodayRecorded(userId, hobby.getId());

        return GetHobbyListByChipResDto.HobbyInfoByChip.of(hobby, todayRecorded);
    }

    private boolean isTodayRecorded(String userId, Long hobbyId) {
        String key = todayRecordRedisService.createRecordKey(userId, hobbyId);
        return todayRecordRedisService.hasKey(key);
    }

    private boolean isDirectUploadCase(SetHobbyCoverImageReqDto reqDto) {
        return reqDto.getHobbyId() != null && StringUtils.hasText(reqDto.getCoverImageUrl());
    }

    private boolean isRecordCase(SetHobbyCoverImageReqDto reqDto) {
        return reqDto.getRecordId() != null;
    }

    /**
     * Case 1: 직접 업로드된 이미지 URL로 설정
     */
    private CoverChangeResult handleDirectUpload(SetHobbyCoverImageReqDto reqDto, User currentUser) {
        Hobby hobby = hobbyUtil.getHobby(reqDto.getHobbyId());
        hobbyUtil.verifyHobbyOwner(hobby, currentUser);

        String newUrl = reqDto.getCoverImageUrl();
        String oldUrl = hobby.getCoverImageUrl();

        if (Objects.equals(oldUrl, newUrl)) {
            return CoverChangeResult.unchanged(hobby.getId(), oldUrl);
        }
        imageLifecyclePort.validateExists(newUrl);
        imageLifecyclePort.deleteAfterCommit(oldUrl);
        hobby.updateCoverImage(newUrl);

        return CoverChangeResult.changed(hobby.getId(), newUrl);
    }

    /**
     * Case 2: 기존 활동 기록의 사진(또는 스티커 기본 이미지)으로 설정
     */
    private CoverChangeResult handleFromRecord(SetHobbyCoverImageReqDto reqDto, User currentUser) throws Exception {
        ActivityRecord record = activityRecordRepository.findByIdWithHobby(reqDto.getRecordId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        if (!Objects.equals(record.getUser(), currentUser)) {
            throw new CustomException(ErrorCode.NOT_ACTIVITY_RECORD_OWNER);
        }

        Hobby hobby = record.getHobby();
        String oldCoverUrl = hobby.getCoverImageUrl();
        String newCoverUrl = buildCoverUrlFromRecord(record);
        imageLifecyclePort.deleteAfterCommit(oldCoverUrl);
        hobby.updateCoverImage(newCoverUrl);

        return CoverChangeResult.changed(hobby.getId(), newCoverUrl);
    }

    /**
     * record의 imageUrl이 있으면 S3 복사 + 람다 리사이즈 생성 후 cover 원본 url 반환
     * 없으면 sticker 기반 기본 cover url 반환
     */
    private String buildCoverUrlFromRecord(ActivityRecord record) throws Exception {
        String recordImageUrl = record.getImageUrl();

        if (StringUtils.hasText(recordImageUrl)) {
            String srcKey = imageUrlPort.extractKeyFromFileUrl(recordImageUrl);

            String newCoverKey = srcKey.replace(TEMP_ACTIVITY_PATH, TEMP_COVER_PATH);
            String resizedCoverKey = newCoverKey.replace(TEMP_DIR, THUMB_DIR);

            imageLifecyclePort.copy(srcKey, newCoverKey);
            coverGeneratorPort.generateCover(newCoverKey, resizedCoverKey);
            return imageUrlPort.createFileUrl(newCoverKey);
        }

        return StickerCover.getUrlBySticker(record.getSticker());
    }

    private void saveRecommendItems(Hobby hobby, FastAPIRecommendResDto response) {
        List<ActivityRecommendItem> items = response.getActivities().stream()
                .map(item -> ActivityRecommendItem.builder()
                        .hobby(hobby)
                        .content(item.getContent())
                        .description(item.getDescription())
                        .build())
                .toList();

        activityRecommendItemRepository.saveAll(items);
    }

    private Hobby resolveHobby(Long hobbyId, User user) {
        if (hobbyId != null) {
            return hobbyRepository.findByIdAndUserId(hobbyId, user.getId()).orElseThrow(() -> new CustomException(ErrorCode.NOT_HOBBY_OWNER));
        }
        return hobbyUtil.getLatestInProgressHobby(user);
    }
}
