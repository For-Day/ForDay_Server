package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.entity.OtherActivity;
import com.example.ForDay.domain.activity.repository.OtherActivityRepository;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.dto.request.*;
import com.example.ForDay.domain.hobby.dto.response.*;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyInfoRepository;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.repository.UserRepository;
import com.example.ForDay.global.ai.service.AiActivityService;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.domain.activity.service.TodayRecordRedisService;
import com.example.ForDay.global.util.UserUtil;
import com.example.ForDay.infra.lambda.invoker.CoverLambdaInvoker;
import com.example.ForDay.infra.s3.service.S3Service;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

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
    private final AiActivityService aiActivityService;
    private final AiCallCountService aiCallCountService;
    private final HobbyInfoRepository hobbyInfoRepository;
    private final RestTemplate restTemplate;
    private final ActivityRecordRepository activityRecordRepository;
    private final TodayRecordRedisService todayRecordRedisService;
    private final UserSummaryAIService userSummaryAIService;
    private final S3Service s3Service;
    private final OtherActivityRepository otherActivityRepository;
    private final CoverLambdaInvoker invoker;
    private final UserRepository userRepository;

    @Transactional
    public ActivityCreateResDto hobbyCreate(ActivityCreateReqDto reqDto, CustomUserDetails user) {
        log.info("[ActivityCreate] 요청 시작 - userId={}, hobbyCardId={}",
                user.getUsername(), reqDto.getHobbyInfoId());

        User currentUser = userUtil.getCurrentUser(user);

        boolean isNicknameSet = StringUtils.hasText(currentUser.getNickname()); // 닉네임 설정 여부
        boolean onboardingCompleted = currentUser.isOnboardingCompleted(); // 온보딩 완료 여부

        if (onboardingCompleted && !isNicknameSet) {
            // 온보딩은 완료 닉네임은 미설정시 (같은 취미에 대한 중복 요청이 있을 것임
            throw new CustomException(ErrorCode.DUPLICATE_HOBBY_REQUEST);
        }

        // 이미 진행 중인 취미가 두개인지 검사
        long hobbyCount = hobbyRepository.countByStatusAndUser(HobbyStatus.IN_PROGRESS, currentUser);
        if (hobbyCount >= 2) {
            throw new CustomException(ErrorCode.MAX_IN_PROGRESS_HOBBY_EXCEEDED);
        }

        Hobby hobby = Hobby.builder()
                .user(currentUser)
                .hobbyInfoId(reqDto.getHobbyInfoId())
                .hobbyName(reqDto.getHobbyName())
                .hobbyPurpose(reqDto.getHobbyPurpose())
                .hobbyTimeMinutes(reqDto.getHobbyTimeMinutes())
                .executionCount(reqDto.getExecutionCount())
                .goalDays(reqDto.getIsDurationSet() ? DEFAULT_GOAL_DAYS : null)
                .status(HobbyStatus.IN_PROGRESS)
                .build();

        hobbyRepository.save(hobby);
        log.info("[ActivityCreate] Hobby 생성 완료 - hobbyId={}, userId={}",
                hobby.getId(), currentUser.getId());

        // 온보딩이 완료되지 않은 경우에만 완료로 전환되도록 설정
        if (!currentUser.isOnboardingCompleted()) {
            log.info("[Before Update] onboarding status: {}", currentUser.isOnboardingCompleted());
            currentUser.completeOnboarding();
            userRepository.save(currentUser);
        }

        return new ActivityCreateResDto("취미가 성공적으로 생성되었습니다.", hobby.getId());
    }

    @Transactional(readOnly = true)
    public ActivityAIRecommendResDto activityAiRecommend(Long hobbyId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        String userId = currentUser.getId();

        Hobby hobby = getHobby(hobbyId);
        verifyHobbyOwner(hobby, currentUser); // hobby의 소유자인지 검증
        checkHobbyInProgressStatus(hobby); // 현재 진행 중인 취미인지 확인

        // 오늘 ai 호출 횟수 조회 (내부 로직에서 3회를 넘어가면 예외 처리됨)
        String socialId = currentUser.getSocialId();
        int currentCount = aiCallCountService.increaseAndGet(socialId, hobbyId);

        log.info("[AI-RECOMMEND][CALL] user={} calling AI model", userId);


        // 2. FastAPI 요청 객체 생성 (추가 select 없음 - 이미 영속성에 있는 상황)
        FastAPIRecommendReqDto requestDto = FastAPIRecommendReqDto.builder()
                .userId(userId)
                .userHobbyId(hobbyId.intValue())
                .hobbyName(hobby.getHobbyName())
                .hobbyPurpose(hobby.getHobbyPurpose())
                .hobbyTimeMinutes(hobby.getHobbyTimeMinutes())
                .executionCount(hobby.getExecutionCount())
                .goalDays(hobby.getGoalDays() != null ? hobby.getGoalDays() : 0)
                .build();

        // 3. FastAPI 호출
        String url = fastApiBaseUrl + "/ai/activities/recommend";
        //String url = fastApiBaseUrl + "/activities/recommend";
        try {
             FastAPIRecommendResDto response = restTemplate.postForObject(url, requestDto, FastAPIRecommendResDto.class);

            if (response == null || response.getActivities().isEmpty()) {
                throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
            }

            String userSummaryText = "";
            long recordCount = activityRecordRepository.countByUserIdAndHobbyId(currentUser.getId(), hobbyId);

            if(recordCount >=5) {
                // 기존에 사용자 요약 문구가 존재하는지 redis에 조회
                if(userSummaryAIService.hasSummary(socialId, hobbyId)) {
                    userSummaryText = userSummaryAIService.getSummary(socialId, hobby.getId());
                } else {
                    // fast api에 요청
                    userSummaryText = fetchAndSaveUserSummary(userId, socialId, hobbyId, hobby.getHobbyName());
                }

            }
            userSummaryText += " 포데이 AI가 알맞은 취미 활동을 추천드려요";

            return new ActivityAIRecommendResDto("AI가 취미 활동을 추천했습니다.", currentCount, maxCallLimit, userSummaryText, response.getActivities());

        } catch (Exception e) {
            aiCallCountService.decrease(socialId, hobbyId);
            log.error("[AI-RECOMMEND][ERROR] FastAPI 호출 실패: {}", e.getMessage());
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

        Long hobbyInfoId = hobby.getHobbyInfoId();

        List<OtherActivity> activities = otherActivityRepository.findRandomThreeByHobbyInfoId(hobbyInfoId);

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

        activityRepository.saveAll(activities);
        log.info("[AddActivity] 성공 - 저장된 활동 수: {}", activities.size());

        return new AddActivityResDto(
                "취미 활동이 정상적으로 생성되었습니다.",
                activities.size()
        );
    }


    @Transactional(readOnly = true)
    public GetHobbyActivitiesResDto getHobbyActivities(Long hobbyId, CustomUserDetails user, Integer size) {
        User currentUser = userUtil.getCurrentUser(user); // 쿼리 0회 (이미 필터에서 로드됨)
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
        log.info("[GetHomeHobbyInfo] 대시보드 조회 - UserId: {}, TargetHobbyId: {}",
                currentUser.getId(), hobbyId == null ? "DEFAULT(Latest)" : hobbyId);

        Hobby targetHobby = (hobbyId != null)
                ? getHobby(hobbyId)
                : getLatestInProgressHobby(currentUser);
        GetHomeHobbyInfoResDto response = hobbyRepository.getHomeHobbyInfo(targetHobby.getId(), currentUser);

        if (response == null) return null;

        // AI 관련 로직 처리
        String socialId = currentUser.getSocialId();
        String userSummaryText = "";
        boolean isAiCallRemaining = true;

        // 호출 가능 횟수 체크
        int currentCount = aiCallCountService.getCurrentCount(socialId, targetHobby.getId());
        if (currentCount >= 3) isAiCallRemaining = false;

        // 요약 문구 처리 (기록 5개 이상일 때)
        long recordCount = activityRecordRepository.countByUserIdAndHobbyId(currentUser.getId(), targetHobby.getId());
        if (recordCount >= 5) {
            if (userSummaryAIService.hasSummary(socialId, targetHobby.getId())) {
                userSummaryText = userSummaryAIService.getSummary(socialId, targetHobby.getId());
            } else {
                userSummaryText = fetchAndSaveUserSummary(currentUser.getId(), socialId, targetHobby.getId(), targetHobby.getHobbyName());
            }
        }

        return response.toBuilder()
                .greetingMessage("반가워요, " + currentUser.getNickname() + "님! 👋")
                .userSummaryText(userSummaryText)
                .recommendMessage("포데이 AI가 알맞은 취미활동을 추천해드려요")
                .aiCallRemaining(isAiCallRemaining)
                .build();
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

        log.info("[HobbyService] 취미 시간 수정 완료 - hobbyId={}, userId={}, before={}, after={}",
                hobbyId,
                hobby.getUser().getId(),
                before,
                dto.getMinutes()
        );

        return new MessageResDto("취미 시간이 수정되었습니다.");
    }


    @Transactional
    public MessageResDto updateExecutionCount(Long hobbyId, ExecutionCountPayload dto, CustomUserDetails user) {
        log.info("[HobbyService] 취미 실행 횟수 수정 요청 - hobbyId={}, executionCount={}",
                hobbyId, dto.getExecutionCount());

        Hobby hobby = checkHobbyUpdateable(hobbyId, user);

        Integer before = hobby.getExecutionCount();
        hobby.updateExecutionCount(dto.getExecutionCount());

        log.info("[HobbyService] 취미 실행 횟수 수정 완료 - hobbyId={}, userId={}, before={}, after={}",
                hobbyId,
                hobby.getUser().getId(),
                before,
                dto.getExecutionCount()
        );

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

        log.info("[HobbyService] 취미 목표 기간 수정 완료 - hobbyId={}, userId={}, before={}, after={}",
                hobbyId,
                hobby.getUser().getId(),
                before,
                after
        );

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
                        hobbyRepository.countByStatusAndUser(
                                HobbyStatus.IN_PROGRESS,
                                currentUser // 현재 유저의 진행 중인 취미가 이미 2개이면 꺼낼 수 없다.
                        );

                if (inProgressCount >= 2) {
                    log.warn("[HobbyService] 진행 중 취미 개수 초과 - userId={}, count={}",
                            currentUser.getId(), inProgressCount);
                    throw new CustomException(ErrorCode.MAX_IN_PROGRESS_HOBBY_EXCEEDED);
                }

                hobby.updateHobbyStatus(HobbyStatus.IN_PROGRESS);
            }

            case ARCHIVED -> hobby.updateHobbyStatus(HobbyStatus.ARCHIVED);

            default -> {
                log.warn("[HobbyService] 잘못된 취미 상태 요청 - hobbyId={}, status={}",
                        hobbyId, targetStatus);
                throw new CustomException(ErrorCode.INVALID_HOBBY_STATUS);
            }
        }

        log.info("[HobbyService] 취미 상태 변경 완료 - hobbyId={}, userId={}, from={}, to={}",
                hobbyId,
                currentUser.getId(),
                currentStatus,
                targetStatus
        );

        return new MessageResDto("취미 상태가 성공적으로 수정되었습니다.");
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
        if(hobby.getCurrentStickerNum() < STICKER_COMPLETE_COUNT) {
            throw new CustomException(ErrorCode.HOBBY_STICKER_NOT_ENOUGH);
        }

        switch (reqDto.getType()) {
            case CONTINUE -> hobby.setGoalDaysExtension();
            case ARCHIVE -> hobby.setHobbyArchived();
            default -> throw new CustomException(ErrorCode.INVALID_HOBBY_EXTENSION_TYPE);

        }

        return new SetHobbyExtensionResDto(hobbyId, reqDto.getType(), "취미 기간 설정이 정상적으로 처리되었습니다.");
    }

    private static boolean isCheckStickerFull(Hobby hobby) {
        return Objects.equals(hobby.getCurrentStickerNum(), STICKER_COMPLETE_COUNT) && Objects.equals(hobby.getGoalDays(), STICKER_COMPLETE_COUNT);
    }

    @Transactional(readOnly = true)
    public GetStickerInfoResDto getStickerInfo(
            Long hobbyId,
            Integer page,
            Integer size,
            CustomUserDetails user
    ) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("getStickerInfo 호출: hobbyId={}, page={}, size={}, userId={}", hobbyId, page, size, currentUser.getId());

        // hobby 조회
        Hobby hobby = (hobbyId != null)
                ? hobbyRepository.findByIdAndUserId(hobbyId, currentUser.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_HOBBY_OWNER))
                : getLatestInProgressHobby(currentUser);
        log.debug("조회된 hobby: {}", hobby);

        // 진행 중 취미 자체가 없는 경우
        if (hobby == null) {
            log.warn("진행 중인 취미가 없음, empty 응답 반환");
            return null;
        }

        // 권한 + 상태 체크
        verifyHobbyOwner(hobby, currentUser);
        checkHobbyInProgressStatus(hobby);

        // 기간 설정 여부
        boolean durationSet = hobby.getGoalDays() != null;
        log.debug("durationSet={}", durationSet);

        // 오늘 기록 여부 (Redis)
        boolean recordedToday =
                todayRecordRedisService.hasKey(
                        todayRecordRedisService.createRecordKey(currentUser.getId(), hobby.getId())
                );
        log.debug("recordedToday={}", recordedToday);

        // 전체 스티커 개수 (빈칸 포함)
        int totalStickerNum = hobby.getCurrentStickerNum();
        int totalSlotCount = totalStickerNum;
        if(!recordedToday) totalSlotCount++; // 오늘 기록한게 없으면 빈칸도 포함
        log.debug("totalStickerNum={}, totalSlotCount={}", totalStickerNum, totalSlotCount);

        // 현재 조회하고자 하는 페이지
        int currentPage = (page == null)
                ? calculateCurrentPage(totalSlotCount, size)
                : page;
        log.debug("currentPage={}", currentPage);

        // 전체 페이지
        int totalPage = ((totalSlotCount - 1) / size) + 1;
        log.debug("totalPage={}", totalPage);

        if(currentPage <= 0 || currentPage > totalPage) {
            log.error("유효하지 않은 페이지 요청: currentPage={}, totalPage={}", currentPage, totalPage);
            throw new CustomException(ErrorCode.INVALID_PAGE_REQUEST);
        }

        // DB에서 실제 스티커 조회 (빈칸 제외)
        List<GetStickerInfoResDto.StickerDto> stickerDto =
                activityRecordRepository.getStickerInfo(
                        hobby.getId(),
                        currentPage,
                        size,
                        currentUser
                );
        log.debug("조회된 스티커 개수={}", stickerDto.size());

        GetStickerInfoResDto result = new GetStickerInfoResDto(
                hobby.getId(),
                durationSet,
                recordedToday,
                currentPage,
                totalPage,
                size,
                totalStickerNum,
                currentPage > 1,
                currentPage < totalPage,
                stickerDto
        );

        log.info("getStickerInfo 결과: {}", result);
        return result;
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

    /**
     * FastAPI에 요약을 요청하고 Redis에 저장하는 전용 메서드
     */
    private String fetchAndSaveUserSummary(String userId, String socialId, Long hobbyId, String hobbyName) {
        try {
            // 1. 요청 DTO 구성
            ActivitySummaryRequest requestDto = ActivitySummaryRequest.builder()
                    .userId(userId)
                    .userHobbyId(hobbyId)
                    .hobbyName(hobbyName)
                    .build();

            String fastapiUrl = fastApiBaseUrl + "/ai/summary";

            // 2. FastAPI 호출 및 DTO 응답 받기
            ActivitySummaryResponse response = restTemplate.postForObject(
                    fastapiUrl,
                    requestDto,
                    ActivitySummaryResponse.class
            );

            // 3. 결과 처리
            if (response != null && response.getSummary() != null) {
                String summary = response.getSummary();

                // Redis에 7일간 저장
                userSummaryAIService.saveSummary(socialId, hobbyId, summary);
                return summary;
            }
        } catch (Exception e) {
            log.error("FastAPI 요약 요청 실패 | socialId: {}, hobbyId: {}, error: {}",
                    socialId, hobbyId, e.getMessage());
        }

        // 예외 발생 시 기본 가이드 문구 반환
        return "";
    }

    @Transactional
    public SetHobbyCoverImageResDto setHobbyCoverImage(@Valid SetHobbyCoverImageReqDto reqDto, CustomUserDetails user) throws Exception {
        User currentUser = userUtil.getCurrentUser(user);
        String updatedUrl;
        Long targetHobbyId;

        // Case 1: 직접 업로드된 이미지 URL로 설정하는 경우
        if (reqDto.getHobbyId() != null && StringUtils.hasText(reqDto.getCoverImageUrl())) {
            Hobby hobby = getHobby(reqDto.getHobbyId());
            verifyHobbyOwner(hobby, currentUser);

            // cover_image/temp/~~~~
            String newCoverImageUrl = reqDto.getCoverImageUrl();
            // cover_image/resized/thumb/~~~~
            String resizedCoverImageUrl = toCoverMainResizedUrl(newCoverImageUrl);

            // S3 존재 여부 검증
            String newCoverImageKey = s3Service.extractKeyFromFileUrl(newCoverImageUrl);
            String resizedCoverImageKey = s3Service.extractKeyFromFileUrl(resizedCoverImageUrl);
            if (!s3Service.existsByKey(newCoverImageKey) && !s3Service.existsByKey(resizedCoverImageKey)) {
                throw new CustomException(ErrorCode.S3_IMAGE_NOT_FOUND);
            }

            // 기존 url 삭제
            String oldCoverImageUrl = hobby.getCoverImageUrl();
            if(oldCoverImageUrl != null && !oldCoverImageUrl.isBlank()) {
                String oldCoverKey = s3Service.extractKeyFromFileUrl(oldCoverImageUrl);
                String resizedCoverUrl = toCoverMainResizedUrl(oldCoverImageUrl);
                String resizedCoverKey = s3Service.extractKeyFromFileUrl(resizedCoverUrl);
                if(s3Service.existsByKey(oldCoverKey)) {
                    s3Service.deleteByKey(oldCoverKey);
                }
                if(s3Service.existsByKey(resizedCoverKey)) {
                    s3Service.deleteByKey(resizedCoverKey);
                }
            }

            hobby.updateCoverImage(newCoverImageUrl); // 원본 url을 db에 저장
            updatedUrl = hobby.getCoverImageUrl();
            targetHobbyId = hobby.getId();
        }
        // Case 2: 기존 활동 기록의 사진으로 설정하는 경우
        else if (reqDto.getRecordId() != null) {
            ActivityRecord activityRecord = activityRecordRepository.findByIdWithHobby(reqDto.getRecordId())
                    .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

            // 권한 확인
            if (!Objects.equals(activityRecord.getUser(), currentUser)) {
                throw new CustomException(ErrorCode.NOT_ACTIVITY_RECORD_OWNER);
            }

            String activityRecordImageUrl = activityRecord.getImageUrl();
            String activityRecordKey = s3Service.extractKeyFromFileUrl(activityRecordImageUrl);

           /* if (!activityRecordKey.startsWith("activity_record/temp/")) {
                throw new CustomException(ErrorCode.INVALID_IMAGE_SOURCE);
            }

            String dstKey = activityRecordKey
                    .replace("activity_record/temp/", "cover_image/resized/thumb/");

            Map<String, Object> payload = new HashMap<>();
            payload.put("action", "SET_COVER");
            payload.put("srcBucket", "forday-s3-bucket");
            payload.put("dstBucket", "forday-s3-bucket");
            payload.put("srcKey", activityRecordKey);   // 예: activity_record/temp/uuid_xxx.jpg
            payload.put("dstKey", dstKey);   // 예: cover_image/resized/thumb/uuid_xxx.jpg
            payload.put("size", 96);         // 48 표시라면 2배 저장
            payload.put("format", "jpeg");

            invoker.invokeSync(payload);

            String oldCoverUrl = hobby.getCoverImageUrl();
            if (oldCoverUrl != null) {
                String oldKey = s3Service.extractKeyFromFileUrl(oldCoverUrl);
                if (s3Service.existsByKey(oldKey)) {
                    s3Service.deleteByKey(oldKey);
                }
            }*/

            Hobby hobby = activityRecord.getHobby();
            // createCoverLambda 를 이용하여 /activity_record/temp/ -> /cover_image/resized/thumb
            hobby.updateCoverImage(activityRecordImageUrl); // 여기도 resize된 url 저장되도록

            updatedUrl = hobby.getCoverImageUrl();
            targetHobbyId = hobby.getId();
        }
        else {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE);
        }

        return new SetHobbyCoverImageResDto(
                "대표 이미지가 성공적으로 변경되었습니다.",
                targetHobbyId,
                reqDto.getRecordId(),
                updatedUrl
        );
    }

    private static String toCoverMainResizedUrl(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains("/temp/")) {
            return originalUrl;
        }
        return originalUrl.replace("/temp/", "/resized/thumb/");
    }


}
