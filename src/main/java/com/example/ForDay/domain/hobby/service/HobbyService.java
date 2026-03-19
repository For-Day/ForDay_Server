package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.entity.ActivityRecommendItem;
import com.example.ForDay.domain.activity.entity.OtherActivity;
import com.example.ForDay.domain.activity.repository.ActivityBulkRepository;
import com.example.ForDay.domain.activity.repository.ActivityRecommendItemRepository;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.activity.repository.OtherActivityRepository;
import com.example.ForDay.domain.activity.service.TodayRecordRedisService;
import com.example.ForDay.domain.hobby.dto.StickerContext;
import com.example.ForDay.domain.hobby.dto.request.*;
import com.example.ForDay.domain.hobby.dto.response.*;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.lambda.invoker.CoverLambdaInvoker;
import com.example.ForDay.infra.s3.service.S3Service;
import com.example.ForDay.infra.s3.util.S3Util;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class HobbyService {
    private static final Integer DEFAULT_GOAL_DAYS = 66;
    private static final Integer STICKER_COMPLETE_COUNT = 66;

    @Value("${ai.max-call-limit}")
    private int maxCallLimit;

    @Value("${fastapi.url}")
    private String fastApiBaseUrl;

    private final HobbyRepository hobbyRepository;
    private final UserUtil userUtil;
    private final ActivityRepository activityRepository;
    private final AiCallCountService aiCallCountService;
    private final RestTemplate restTemplate;
    private final ActivityRecordRepository activityRecordRepository;
    private final TodayRecordRedisService todayRecordRedisService;
    private final UserSummaryAIService userSummaryAIService;
    private final S3Service s3Service;
    private final OtherActivityRepository otherActivityRepository;
    private final CoverLambdaInvoker invoker;
    private final UserRepository userRepository;
    private final S3Util s3Util;
    private final ActivityBulkRepository activityBulkRepository;
    private final ActivityRecommendItemRepository activityRecommendItemRepository;

    @Transactional
    public ActivityCreateResDto hobbyCreate(ActivityCreateReqDto reqDto, CustomUserDetails userDetails) {
        User currentUser = userUtil.getCurrentUser(userDetails);
        log.info("[ActivityCreate] Start - userId={}, hobbyId={}, hobbyName={}",
                currentUser.getId(), reqDto.getHobbyInfoId(), reqDto.getHobbyName());

        // 온보딩 및 닉네임 상태 검증
        validateUserStatus(currentUser);
        // 진행 중인 취미 개수 제한 검증
        validateMaxInProgressHobbies(currentUser);
        // 취미 중복 여부 검증
        validateDuplicateHobby(reqDto, currentUser);
        // 취미 엔티티 생성 및 저장
        Hobby savedHobby = hobbyRepository.save(Hobby.createNewHobby(currentUser, reqDto, DEFAULT_GOAL_DAYS));
        // 온보딩 상태 업데이트
        handleUserOnboarding(currentUser);

        return new ActivityCreateResDto("취미가 성공적으로 생성되었습니다.", savedHobby.getId());
    }

    @Transactional
    public ActivityAIRecommendResDto activityAiRecommend(Long hobbyId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        Hobby hobby = validateHobbyAccess(hobbyId, currentUser);

        String socialId = currentUser.getSocialId();
        int currentCount = aiCallCountService.increaseAndGet(socialId, hobbyId);

        try {
            FastAPIRecommendResDto response = requestAI(currentUser, hobby);
            String summary = buildUserSummary(currentUser, hobby, socialId);
            saveRecommendItems(hobby, response);

            return createResponse(currentCount, summary, response);
        } catch (Exception e) {
            aiCallCountService.decrease(socialId, hobbyId);
            log.error("[AI-RECOMMEND][ERROR] {}", e.getMessage());
            throw new CustomException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    @Transactional(readOnly = true)
    public ActivityAIRecommendResDto testActivityAiRecommend(Long hobbyId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        Hobby hobby = getHobby(hobbyId);
        verifyHobbyOwner(hobby, currentUser); // hobby의 소유자인지 검증
        checkHobbyInProgressStatus(hobby); // 현재 진행 중인 취미인지 확인

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
    public OthersActivityRecommendResDto othersActivityRecommendV1(Long hobbyId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        Hobby hobby = hobbyRepository.findByIdAndUserId(hobbyId, currentUser.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.HOBBY_NOT_FOUND));

        List<OtherActivity> activities = otherActivityRepository.findRandomThreeByHobbyInfoId(hobby.getHobbyInfoId());

        List<OthersActivityRecommendResDto.ActivityDto> list = activities.stream()
                .map(OthersActivityRecommendResDto.ActivityDto::from)
                .toList();

        return new OthersActivityRecommendResDto("다른 하비들이 많이 하는 활동 목록 조회에 성공하셨습니다.", list);
    }

    @Transactional
    public AddActivityResDto addActivity(Long hobbyId, AddActivityReqDto reqDto, CustomUserDetails user) {
        Hobby hobby = getHobby(hobbyId);
        User currentUser = userUtil.getCurrentUser(user);
        verifyHobbyOwner(hobby, currentUser); // 취미 소유자인지 검증
        checkHobbyInProgressStatus(hobby); // 현재 진행 중인 취미인지

        log.info("[AddActivity] 시작 - UserId: {}, HobbyId: {}, 요청 활동 수: {}",
                currentUser.getId(), hobbyId, reqDto.getActivities().size());

        List<Activity> activities = reqDto.getActivities().stream()
                .map(activity -> Activity.builder()
                        .user(currentUser)
                        .hobby(hobby)
                        .content(activity.getContent())
                        .aiRecommended(activity.isAiRecommended())
                        .build()
                )
                .toList();

        activityBulkRepository.bulkInsertActivities(activities);
        log.info("[AddActivity] 성공 - 저장된 활동 수: {}", activities.size());

        return new AddActivityResDto("취미 활동이 정상적으로 생성되었습니다.", activities.size()
        );
    }

    @Transactional(readOnly = true)
    public GetHobbyActivitiesResDto getHobbyActivities(Long hobbyId, CustomUserDetails user, Integer size) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[GetHobbyActivities] 조회 시작 - UserId: {}, HobbyId: {}", currentUser.getId(), hobbyId);

        if (!hobbyRepository.existsByIdAndUserId(hobbyId, currentUser.getId())) {
            throw new CustomException(ErrorCode.NOT_HOBBY_OWNER);
        }

        GetHobbyActivitiesResDto response = activityRepository.getHobbyActivities(hobbyId, size);
        log.info("[GetHobbyActivities] 조회 완료 - 활동 개수: {}", response.getActivities().size());
        return response;
    }

    @Transactional(readOnly = true)
    public GetHomeHobbyInfoResDto getHomeHobbyInfo(Long hobbyId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[GetHomeHobbyInfo] Dashboard inquiry - UserId: {}, TargetHobbyId: {}",
                currentUser.getId(), hobbyId == null ? "DEFAULT(Latest)" : hobbyId);

        // 대상 취미 결정
        Hobby targetHobby = (hobbyId != null) ? getHobby(hobbyId) : getLatestInProgressHobby(currentUser);

        // 취미가 없는 경우 기본 응답 반환
        if (targetHobby == null) {
            return createDefaultResponse(currentUser.getNickname());
        }

        // DB에서 대시보드 기초 정보 조회
        GetHomeHobbyInfoResDto response = hobbyRepository.getHomeHobbyInfo(targetHobby.getId(), currentUser);
        if (response == null) {
            log.warn("[GetHomeHobbyInfo] Failed to fetch hobby data - HobbyId: {}", targetHobby.getId());
            return createDefaultResponse(currentUser.getNickname());
        }

        // AI 관련 데이터(요약, 호출 횟수) 설정
        return buildFinalResponse(response, currentUser, targetHobby);
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
        log.info("[HobbyService] 취미 시간 수정 요청 - hobbyId={}, minutes={}",
                hobbyId, dto.getMinutes());

        Hobby hobby = checkHobbyUpdateable(hobbyId, user);

        Integer before = hobby.getHobbyTimeMinutes();
        hobby.updateHobbyTimeMinutes(dto.getMinutes());

        log.info("[HobbyService] 취미 시간 수정 완료 - hobbyId={}, userId={}, before={}, after={}",hobbyId, hobby.getUser().getId(), before, dto.getMinutes());
        return new MessageResDto("취미 시간이 수정되었습니다.");
    }


    @Transactional
    public MessageResDto updateExecutionCount(Long hobbyId, ExecutionCountPayload dto, CustomUserDetails user) {
        log.info("[HobbyService] 취미 실행 횟수 수정 요청 - hobbyId={}, executionCount={}",
                hobbyId, dto.getExecutionCount());

        Hobby hobby = checkHobbyUpdateable(hobbyId, user);

        Integer before = hobby.getExecutionCount();
        hobby.updateExecutionCount(dto.getExecutionCount());

        log.info("[HobbyService] 취미 실행 횟수 수정 완료 - hobbyId={}, userId={}, before={}, after={}",hobbyId, hobby.getUser().getId(), before, dto.getExecutionCount());
        return new MessageResDto("실행 횟수가 수정되었습니다.");
    }

    @Transactional
    public MessageResDto updateGoalDays(Long hobbyId, GoalDaysPayload dto, CustomUserDetails user) {
        log.info("[HobbyService] 취미 목표 기간 수정 요청 - hobbyId={}, isDurationSet={}",
                hobbyId, dto.getIsDurationSet());

        Hobby hobby = checkHobbyUpdateable(hobbyId, user);
        Integer before = hobby.getGoalDays();
        Integer after = dto.getIsDurationSet() ? 66 : null;

        hobby.updateGoalDays(after);
        log.info("[HobbyService] 취미 목표 기간 수정 완료 - hobbyId={}, userId={}, before={}, after={}",hobbyId, hobby.getUser().getId(), before, after);
        return new MessageResDto("목표 기간 설정이 수정되었습니다.");
    }

    // 진행중 -> 보관, 보관 -> 진행중
    @Transactional
    public MessageResDto updateHobbyStatus(
            Long hobbyId,
            UpdateHobbyStatusReqDto reqDto,
            CustomUserDetails user
    ) {
        log.info("[HobbyService] 취미 상태 변경 요청 - hobbyId={}, targetStatus={}",
                hobbyId, reqDto.getHobbyStatus());

        Hobby hobby = getHobby(hobbyId);
        User currentUser = userUtil.getCurrentUser(user);

        verifyHobbyOwner(hobby, currentUser);

        HobbyStatus currentStatus = hobby.getStatus(); // 현재 상태
        HobbyStatus targetStatus = reqDto.getHobbyStatus(); // 바꾸려는 상태

        // 동일 상태 요청
        if (currentStatus == targetStatus) {
            log.info("[HobbyService] 취미 상태 변경 요청 무시 (동일 상태) - hobbyId={}, status={}",
                    hobbyId, currentStatus);
            return new MessageResDto("이미 해당 상태입니다.");
        }

        switch (targetStatus) {
            case IN_PROGRESS -> { // 보관 -> 진행
                long inProgressCount =
                        hobbyRepository.countByStatusAndUser(HobbyStatus.IN_PROGRESS, currentUser);
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
        Hobby hobby = getHobby(hobbyId);
        User currentUser = userUtil.getCurrentUser(user);
        verifyHobbyOwner(hobby, currentUser);    // 원래 기간 설정이 안된 취미의 경우

        // 기간 미설정 취미
        if (hobby.getGoalDays() == null) {
            throw new CustomException(ErrorCode.HOBBY_PERIOD_NOT_SET);
        }

        // 스티커 미완성
        if (hobby.getCurrentStickerNum() < STICKER_COMPLETE_COUNT) {
            throw new CustomException(ErrorCode.HOBBY_STICKER_NOT_ENOUGH);
        }

        switch (reqDto.getType()) {
            case CONTINUE -> hobby.setGoalDaysExtension();
            case ARCHIVE -> hobby.setHobbyArchived();
            default -> throw new CustomException(ErrorCode.INVALID_HOBBY_EXTENSION_TYPE);

        }
        return new SetHobbyExtensionResDto(hobbyId, reqDto.getType(), "취미 기간 설정이 정상적으로 처리되었습니다.");
    }

    @Transactional(readOnly = true)
    public GetStickerInfoResDto getStickerInfo(
            Long hobbyId,
            Integer page,
            Integer size,
            CustomUserDetails user
    ) {
        User currentUser = userUtil.getCurrentUser(user);

        Hobby hobby = resolveHobby(hobbyId, currentUser);
        validateHobby(hobby, currentUser);

        StickerContext context = buildStickerContext(hobby, currentUser, page, size);

        List<GetStickerInfoResDto.StickerDto> stickers =
                fetchStickers(hobby, context, currentUser);

        return createResponse(hobby, context, stickers);
    }

    @Transactional
    public SetHobbyCoverImageResDto setHobbyCoverImage(SetHobbyCoverImageReqDto reqDto,
                                                       CustomUserDetails user) throws Exception {
        User currentUser = userUtil.getCurrentUser(user);

        CoverChangeResult result;
        if (isDirectUploadCase(reqDto)) {
            result = handleDirectUpload(reqDto, currentUser);
        } else if (isRecordCase(reqDto)) {
            result = handleFromRecord(reqDto, currentUser);
        } else {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return new SetHobbyCoverImageResDto(
                "대표사진 설정 완료!",
                result.hobbyId(),
                result.recordId(),
                s3Util.toCoverMainResizedUrl(result.updatedCoverUrl())
        );
    }

    @Transactional(readOnly = true)
    public CanCreateHobbyResDto canCreateHobby(String name, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        if (hobbyRepository.existsByHobbyNameAndUserId(name, currentUser.getId())) {
            return new CanCreateHobbyResDto("이미 등록한 취미입니다.", false);
        }
        return new CanCreateHobbyResDto("등록 가능한 취미입니다.", true);
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

        Hobby hobby = hobbyRepository.findByIdAndUserId(hobbyId, currentUser.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.HOBBY_NOT_FOUND));

        hobby.updateHobby(reqDto.getHobbyInfoId(), reqDto.getHobbyName(), reqDto.getHobbyPurpose(), reqDto.getHobbyTimeMinutes(), reqDto.getExecutionCount(), reqDto.isDurationSet() ? DEFAULT_GOAL_DAYS : null);

        return new UpdateHobbyResDto(hobby.getId(), hobby.getHobbyInfoId(), hobby.getHobbyName(), hobby.getHobbyPurpose(), hobby.getHobbyTimeMinutes(), hobby.getExecutionCount(), hobby.getGoalDays());
    }

    @Transactional(readOnly = true)
    public GetHobbyListByChipResDto getHobbyListByChip(HobbyStatus status, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        // 취미 조회
        List<Hobby> hobbies = getHobbiesByStatus(currentUser.getId(), status);

        // DTO 변환 (오늘 기록 여부 포함)
        List<GetHobbyListByChipResDto.HobbyInfoByChip> hobbyInfos =
                hobbies.stream()
                        .map(hobby -> toHobbyInfo(currentUser.getId(), hobby))
                        .toList();
        return new GetHobbyListByChipResDto(hobbyInfos);
    }

    private List<Hobby> getHobbiesByStatus(String userId, HobbyStatus status) {
        if (status == HobbyStatus.ALL) {
            return hobbyRepository.findAllByUserIdOrderByIdDesc(userId);
        }
        return hobbyRepository.findAllByUserIdAndStatusOrderByIdDesc(userId, status);
    }

    private GetHobbyListByChipResDto.HobbyInfoByChip toHobbyInfo(String userId, Hobby hobby) {

        boolean todayRecorded = isTodayRecorded(userId, hobby.getId());

        return new GetHobbyListByChipResDto.HobbyInfoByChip(
                hobby.getId(),
                hobby.getHobbyName(),
                todayRecorded
        );
    }

    private boolean isTodayRecorded(String userId, Long hobbyId) {
        String key = todayRecordRedisService.createRecordKey(userId, hobbyId);
        return todayRecordRedisService.hasKey(key);
    }

    private GetHomeHobbyInfoResDto buildFinalResponse(GetHomeHobbyInfoResDto response, User user, Hobby hobby) {
        String socialId = user.getSocialId();
        Long hobbyId = hobby.getId();

        // AI 호출 가능 횟수 체크
        int currentCount = aiCallCountService.getCurrentCount(socialId, hobbyId);
        boolean isAiCallRemaining = currentCount < maxCallLimit;

        // AI 요약 문구 생성/조회
        String userSummaryText = determineAiSummary(user, hobby);

        log.info("[GetHomeHobbyInfo] Completion - UserId: {}, Hobby: {}, AI Success: {}",
                user.getId(), hobby.getHobbyName(), !userSummaryText.isEmpty());

        return response.toBuilder()
                .greetingMessage("반가워요, " + user.getNickname() + "님! 👋")
                .userSummaryText(userSummaryText)
                .recommendMessage("포데이 AI가 알맞은 취미활동을 추천해드려요")
                .aiCallRemaining(isAiCallRemaining)
                .aiCallRemainingCount(maxCallLimit - currentCount)
                .nickname(user.getNickname())
                .build();
    }

    private String determineAiSummary(User user, Hobby hobby) {
        // 최근 7일간 기록 개수 확인
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long recordCount = activityRecordRepository.countByUserIdAndHobbyIdAndCreatedAtAfterAndDeletedFalse(
                user.getId(), hobby.getId(), sevenDaysAgo
        );

        if (recordCount < 5) {
            log.info("[GetHomeHobbyInfo] Insufficient records for AI summary (Count: {})", recordCount);
            return "";
        }

        // 캐시된 요약 확인 또는 신규 생성
        if (userSummaryAIService.hasSummary(user.getSocialId(), hobby.getId())) {
            return userSummaryAIService.getSummary(user.getSocialId(), hobby.getId());
        }

        try {
            return userSummaryAIService.fetchAndSaveUserSummary(user.getId(), user.getSocialId(), hobby.getId(), hobby.getHobbyName());
        } catch (Exception e) {
            log.error("Error creating AI summary: {}", e.getMessage());
            return "";
        }
    }

    private GetHomeHobbyInfoResDto createDefaultResponse(String nickname) {
        return new GetHomeHobbyInfoResDto(
                List.of(), null,
                "반가워요, " + nickname + "님! 👋",
                "",
                "포데이 AI가 알맞은 취미활동을 추천해드려요",
                false, 0, null
        );
    }

    private Hobby getHobby(Long hobbyId) {
        return hobbyRepository.findById(hobbyId).orElseThrow(() -> new CustomException(ErrorCode.HOBBY_NOT_FOUND));
    }

    private void verifyHobbyOwner(Hobby hobby, User currentUser) {
        if (!hobby.getUser().getId().equals(currentUser.getId())) {
            throw new CustomException(ErrorCode.NOT_HOBBY_OWNER);
        }
    }

    private Hobby checkHobbyUpdateable(Long hobbyId, CustomUserDetails user) {
        Hobby hobby = getHobby(hobbyId);
        User currentUser = userUtil.getCurrentUser(user);

        verifyHobbyOwner(hobby, currentUser);

        if (!hobby.isUpdatable()) {
            throw new CustomException(ErrorCode.INVALID_HOBBY_STATUS);
        }
        return hobby;
    }

    private void checkHobbyInProgressStatus(Hobby hobby) {
        if (!hobby.getStatus().equals(HobbyStatus.IN_PROGRESS)) {
            throw new CustomException(ErrorCode.INVALID_HOBBY_STATUS);
        }
    }

    private int calculateCurrentPage(int totalSlotCount, int size) {
        if (totalSlotCount <= 0) return 1;
        return ((totalSlotCount - 1) / size) + 1; // total = 10개 -> 1페이지, total = 29 (28+1) -> 2페이지
    }

    private Hobby getLatestInProgressHobby(User user) {
        return hobbyRepository
                .findTopByUserIdAndStatusOrderByCreatedAtDesc(
                        user.getId(),
                        HobbyStatus.IN_PROGRESS
                )
                .orElse(null);
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
        Hobby hobby = getHobby(reqDto.getHobbyId());
        verifyHobbyOwner(hobby, currentUser);

        String newUrl = reqDto.getCoverImageUrl();
        String oldUrl = hobby.getCoverImageUrl();

        // 동일 이미지면 바로 응답
        if (Objects.equals(oldUrl, newUrl)) {
            return new CoverChangeResult(hobby.getId(), null, oldUrl, true);
        }

        // 새 이미지 유효성 검증
        String key = s3Service.extractKeyFromFileUrl(newUrl);
        if (!s3Service.existsByKey(key)) {
            throw new CustomException(ErrorCode.S3_IMAGE_NOT_FOUND);
        }

        // 기존 이미지 삭제(afterCommit)
        registerDeleteCoverAfterCommit(oldUrl);

        // DB 업데이트
        hobby.updateCoverImage(newUrl);

        return new CoverChangeResult(hobby.getId(), null, newUrl, false);
    }

    /**
     * Case 2: 기존 활동 기록의 사진(또는 스티커 기본 이미지)으로 설정
     */
    private CoverChangeResult handleFromRecord(SetHobbyCoverImageReqDto reqDto, User currentUser) throws Exception {
        ActivityRecord record = activityRecordRepository.findByIdWithHobby(reqDto.getRecordId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        // 권한 확인
        if (!Objects.equals(record.getUser(), currentUser)) {
            throw new CustomException(ErrorCode.NOT_ACTIVITY_RECORD_OWNER);
        }

        Hobby hobby = record.getHobby();
        String oldCoverUrl = hobby.getCoverImageUrl();

        String newCoverUrl = buildCoverUrlFromRecord(record); // 핵심: "새 커버 URL 계산"만 담당

        // 기존 이미지 삭제(afterCommit)
        registerDeleteCoverAfterCommit(oldCoverUrl);

        // DB 업데이트
        hobby.updateCoverImage(newCoverUrl);

        return new CoverChangeResult(hobby.getId(), reqDto.getRecordId(), newCoverUrl, false);
    }

    /**
     * record의 imageUrl이 있으면 S3 복사 + 람다 리사이즈 생성 후 cover 원본 url 반환
     * 없으면 sticker 기반 기본 cover url 반환
     */
    private String buildCoverUrlFromRecord(ActivityRecord record) throws Exception {
        String recordImageUrl = record.getImageUrl();

        if (StringUtils.hasText(recordImageUrl)) {
            String srcKey = s3Service.extractKeyFromFileUrl(recordImageUrl);

            String newCoverKey = srcKey.replace("activity_record/temp/", "cover_image/temp/");
            String resizedCoverKey = newCoverKey.replace("/temp/", "/resized/thumb/");

            s3Service.copyObject(srcKey, newCoverKey);

            Map<String, Object> payload = new HashMap<>();
            payload.put("action", "SET_COVER");
            payload.put("srcKey", newCoverKey);
            payload.put("dstKey", resizedCoverKey);

            invoker.invokeSync(payload);

            return s3Service.createFileUrl(newCoverKey);
        }

        return defaultCoverUrlBySticker(record.getSticker());
    }

    /**
     * 기본 스티커 커버 선택
     */
    private String defaultCoverUrlBySticker(String sticker) {
        String s = (sticker == null) ? "" : sticker;

        if (s.contains("smile"))
            return "https://forday-s3-bucket.s3.ap-northeast-2.amazonaws.com/default_cover/smile.png";
        if (s.contains("sad")) return "https://forday-s3-bucket.s3.ap-northeast-2.amazonaws.com/default_cover/sad.png";
        if (s.contains("laugh"))
            return "https://forday-s3-bucket.s3.ap-northeast-2.amazonaws.com/default_cover/laugh.png";
        return "https://forday-s3-bucket.s3.ap-northeast-2.amazonaws.com/default_cover/angry.png";
    }

    /**
     * (원본 + 리사이즈 thumb) 커버 이미지 삭제를 afterCommit으로 등록
     */
    private void registerDeleteCoverAfterCommit(String coverUrl) {
        if (!StringUtils.hasText(coverUrl)) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    String oldKey = s3Service.extractKeyFromFileUrl(coverUrl);
                    s3Service.deleteByKey(oldKey);

                    String resizedUrl = s3Util.toCoverMainResizedUrl(coverUrl);
                    String resizedKey = s3Service.extractKeyFromFileUrl(resizedUrl);
                    s3Service.deleteByKey(resizedKey);
                } catch (Exception e) {
                    log.error("기존 커버 이미지 S3 삭제 실패: {}", coverUrl, e);
                }
            }
        });
    }

    /**
     * 결과 묶음 (가독성 위해 record 사용)
     */
    private record CoverChangeResult(Long hobbyId, Long recordId, String updatedCoverUrl, boolean unchanged) {
    }

    private void validateUserStatus(User user) {
        boolean isNicknameSet = StringUtils.hasText(user.getNickname());
        if (user.isOnboardingCompleted() && !isNicknameSet) {
            throw new CustomException(ErrorCode.DUPLICATE_HOBBY_REQUEST);
        }
    }

    private void validateMaxInProgressHobbies(User user) {
        long hobbyCount = hobbyRepository.countByStatusAndUser(HobbyStatus.IN_PROGRESS, user);
        if (hobbyCount >= 2) {
            throw new CustomException(ErrorCode.MAX_IN_PROGRESS_HOBBY_EXCEEDED);
        }
    }

    private void validateDuplicateHobby(ActivityCreateReqDto reqDto, User user) {
        // ID 기반 중복 체크
        if (reqDto.getHobbyInfoId() != null && reqDto.getHobbyInfoId() >= 1) {
            if (hobbyRepository.existsByHobbyInfoIdAndUserId(reqDto.getHobbyInfoId(), user.getId())) {
                throw new CustomException(ErrorCode.ALREADY_HAVE_HOBBY);
            }
        }
        // 이름 기반 중복 체크
        if (StringUtils.hasText(reqDto.getHobbyName())) {
            if (hobbyRepository.existsByHobbyNameAndUserId(reqDto.getHobbyName(), user.getId())) {
                throw new CustomException(ErrorCode.ALREADY_HAVE_HOBBY);
            }
        }
    }

    private void handleUserOnboarding(User user) {
        if (!user.isOnboardingCompleted()) {
            user.completeOnboarding();
            log.info("[ActivityCreate] User onboarding marked as completed: userId={}", user.getId());
            // @Transactional 환경에서는 영속성 컨텍스트 덕분에 userRepository.save()가 없어도
            // 메서드 종료 시 변경 감지(Dirty Checking)로 자동 Update 쿼리가 나갑니다.
        }
    }

    private Hobby validateHobbyAccess(Long hobbyId, User user) {
        Hobby hobby = getHobby(hobbyId);
        verifyHobbyOwner(hobby, user);
        checkHobbyInProgressStatus(hobby);
        return hobby;
    }

    private FastAPIRecommendResDto requestAI(User user, Hobby hobby) {
        String url = fastApiBaseUrl + "/ai/activities/recommend";
        FastAPIRecommendReqDto requestDto = FastAPIRecommendReqDto.from(user, hobby);

        FastAPIRecommendResDto response =
                restTemplate.postForObject(url, requestDto, FastAPIRecommendResDto.class);

        if (response == null || response.getActivities().isEmpty()) {
            throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
        }

        return response;
    }

    private String buildUserSummary(User user, Hobby hobby, String socialId) {

        long recordCount = activityRecordRepository.countByUserIdAndHobbyId(user.getId(), hobby.getId());

        if (recordCount < 5) {
            return "포데이 AI가 알맞은 취미 활동을 추천드려요";
        }

        String summary;

        if (userSummaryAIService.hasSummary(socialId, hobby.getId())) {
            summary = userSummaryAIService.getSummary(socialId, hobby.getId());
        } else {
            summary = userSummaryAIService.fetchAndSaveUserSummary(
                    user.getId(),
                    socialId,
                    hobby.getId(),
                    hobby.getHobbyName()
            );
        }

        return summary + " 포데이 AI가 알맞은 취미 활동을 추천드려요";
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

    private ActivityAIRecommendResDto createResponse(
            int currentCount,
            String summary,
            FastAPIRecommendResDto response
    ) {
        return new ActivityAIRecommendResDto(
                "AI가 취미 활동을 추천했습니다.",
                currentCount,
                maxCallLimit,
                summary,
                response.getActivities()
        );
    }

    private Hobby resolveHobby(Long hobbyId, User user) {
        if (hobbyId != null) {
            return hobbyRepository.findByIdAndUserId(hobbyId, user.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.NOT_HOBBY_OWNER));
        }
        return getLatestInProgressHobby(user);
    }

    private void validateHobby(Hobby hobby, User user) {
        if (hobby == null) {
            throw new CustomException(ErrorCode.HOBBY_NOT_FOUND);
        }
        verifyHobbyOwner(hobby, user);
        checkHobbyInProgressStatus(hobby);
    }

    private StickerContext buildStickerContext(
            Hobby hobby,
            User user,
            Integer page,
            Integer size
    ) {
        boolean durationSet = hobby.getGoalDays() != null;

        boolean recordedToday = todayRecordRedisService.hasKey(
                todayRecordRedisService.createRecordKey(user.getId(), hobby.getId())
        );

        int totalStickerNum = hobby.getCurrentStickerNum();
        int totalSlotCount = calculateTotalSlot(totalStickerNum, recordedToday);

        int currentPage = resolvePage(page, totalSlotCount, size);
        int totalPage = calculateTotalPage(totalSlotCount, size);

        validatePage(currentPage, totalPage);

        return StickerContext.builder()
                .durationSet(durationSet)
                .recordedToday(recordedToday)
                .totalStickerNum(totalStickerNum)
                .totalSlotCount(totalSlotCount)
                .currentPage(currentPage)
                .totalPage(totalPage)
                .size(size)
                .build();
    }

    private int calculateTotalSlot(int totalStickerNum, boolean recordedToday) {
        return recordedToday ? totalStickerNum : totalStickerNum + 1;
    }

    private int calculateTotalPage(int totalSlotCount, int size) {
        return ((totalSlotCount - 1) / size) + 1;
    }

    private int resolvePage(Integer page, int totalSlotCount, int size) {
        if (page != null) return page;
        return calculateCurrentPage(totalSlotCount, size);
    }

    private void validatePage(int currentPage, int totalPage) {
        if (currentPage <= 0 || currentPage > totalPage) {
            throw new CustomException(ErrorCode.INVALID_PAGE_REQUEST);
        }
    }

    private List<GetStickerInfoResDto.StickerDto> fetchStickers(
            Hobby hobby,
            StickerContext context,
            User user
    ) {
        return activityRecordRepository.getStickerInfo(
                hobby.getId(),
                context.getCurrentPage(),
                context.getSize(),
                user
        );
    }

    private GetStickerInfoResDto createResponse(
            Hobby hobby,
            StickerContext ctx,
            List<GetStickerInfoResDto.StickerDto> stickers
    ) {
        return new GetStickerInfoResDto(
                hobby.getId(),
                ctx.isDurationSet(),
                ctx.isRecordedToday(),
                ctx.getCurrentPage(),
                ctx.getTotalPage(),
                ctx.getSize(),
                ctx.getTotalStickerNum(),
                ctx.getCurrentPage() > 1,
                ctx.getCurrentPage() < ctx.getTotalPage(),
                stickers
        );
    }
}
