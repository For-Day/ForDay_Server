package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRecordRepository;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.dto.request.*;
import com.example.ForDay.domain.hobby.dto.response.*;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyCardRepository;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.ai.dto.response.AiOthersActivityResult;
import com.example.ForDay.global.ai.service.AiActivityService;
import com.example.ForDay.global.ai.service.AiCallCountService;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.RedisUtil;
import com.example.ForDay.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

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
    private final HobbyCardRepository hobbyCardRepository;
    private final RestTemplate restTemplate;
    private final ActivityRecordRepository activityRecordRepository;
    private final RedisUtil redisUtil;

    @Transactional
    public ActivityCreateResDto hobbyCreate(ActivityCreateReqDto reqDto, CustomUserDetails user) {
        log.info("[ActivityCreate] 요청 시작 - userId={}, hobbyCardId={}",
                user.getUsername(), reqDto.getHobbyCardId());

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
                .hobbyCardId(reqDto.getHobbyCardId())
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
            currentUser.completeOnboarding();
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
        int currentCount = aiCallCountService.increaseAndGet(currentUser.getSocialId(), hobbyId);

        log.info("[AI-RECOMMEND][CALL] user={} calling AI model", userId);


        // 2. FastAPI 요청 객체 생성 (추가 select 없음 - 이미 영속성에 있는 상황)
        FastAPIRecommendReq requestDto = FastAPIRecommendReq.builder()
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
        try {
            // FastAPI 응답 타입이 ActivityAIRecommendResDto와 동일하므로 바로 매핑
            ActivityAIRecommendResDto response = restTemplate.postForObject(url, requestDto, ActivityAIRecommendResDto.class);

            if (response == null || response.getActivities().isEmpty()) {
                throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
            }

            // 응답 값 내의 count 정보만 현재 트래킹 값으로 업데이트하여 반환
            response.setAiCallCount(currentCount);
            response.setAiCallLimit(maxCallLimit);
            response.setMessage("AI가 취미 활동을 추천했습니다.");
            return response;

        } catch (Exception e) {
            log.error("[AI-RECOMMEND][ERROR] FastAPI 호출 실패: {}", e.getMessage());
            throw new CustomException(ErrorCode.AI_SERVICE_ERROR);
        }
    }


    @Transactional(readOnly = true)
    public OthersActivityRecommendResDto othersActivityRecommendV1(Long hobbyId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        Hobby hobby = getHobby(hobbyId);
        verifyHobbyOwner(hobby, currentUser);
        checkHobbyInProgressStatus(hobby);

        Long hobbyCardId = hobby.getHobbyCardId();

        log.info("[OTHERS-AI-RECOMMEND][START] hobbyCardId={}", hobbyCardId);

        // 1. HobbyCard 존재 여부 검증
        if (!hobbyCardRepository.existsById(hobbyCardId)) {
            log.warn("[OTHERS-AI-RECOMMEND][NOT-FOUND] hobbyCardId={} is not exist", hobbyCardId);
            throw new CustomException(ErrorCode.HOBBY_CARD_NOT_FOUND);
        }

        // 2. AI 추천 서비스 호출
        log.info("[OTHERS-AI-RECOMMEND][AI-CALL] Calling AI service for hobby: {}", hobbyCardId);
        AiOthersActivityResult aiResult = aiActivityService.othersActivityRecommend(hobby);

        // 3. 응답 결과 검증
        if (aiResult.getOtherActivities() == null || aiResult.getOtherActivities().isEmpty()) {
            log.error("[OTHERS-AI-RECOMMEND][INVALID-RESPONSE] AI returned null or empty result for hobby: {}",
                    hobbyCardId);
            throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
        }

        // 4. DTO 매핑
        log.debug("[OTHERS-AI-RECOMMEND][MAPPING] Mapping AI result to DTO. Size: {}",
                aiResult.getOtherActivities().size());

        List<OthersActivityRecommendResDto.ActivityDto> activities = aiResult.getOtherActivities().stream()
                .map(routine -> new OthersActivityRecommendResDto.ActivityDto(
                        routine.getId(),
                        routine.getContent()
                ))
                .toList();

        // 5. 성공 로그 및 반환
        log.info("[OTHERS-AI-RECOMMEND][SUCCESS] Successfully generated activities for hobbyCardId={}, count={}",
                hobbyCardId,
                activities.size()
        );

        return new OthersActivityRecommendResDto(
                "다른 포비들의 인기 활동을 조회했습니다.",
                activities
        );
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
        Hobby hobby = getHobby(hobbyId);
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[GetHobbyActivities] 조회 시작 - UserId: {}, HobbyId: {}", currentUser.getId(), hobbyId);

        // 현재 사용자가 hobby의 소유자인지 판별
        verifyHobbyOwner(hobby, currentUser);
        checkHobbyInProgressStatus(hobby);

        GetHobbyActivitiesResDto response = activityRepository.getHobbyActivities(hobby, size);
        log.info("[GetHobbyActivities] 조회 완료 - 활동 개수: {}", response.getActivities().size());
        return response; // 해당 취미에 대한 활동 목록 조회

    }

    @Transactional(readOnly = true)
    public GetHomeHobbyInfoResDto getHomeHobbyInfo(Long hobbyId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[GetHomeHobbyInfo] 대시보드 조회 - UserId: {}, TargetHobbyId: {}",
                currentUser.getId(), hobbyId == null ? "DEFAULT(Latest)" : hobbyId);

        return hobbyRepository.getHomeHobbyInfo(hobbyId, currentUser);
    }

    @Transactional(readOnly = true)
    public MyHobbySettingResDto myHobbySetting(CustomUserDetails user, HobbyStatus hobbyStatus) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[MyHobbySetting] 취미 설정 목록 조회 - UserId: {}", currentUser.getId());

        return hobbyRepository.myHobbySetting(currentUser, hobbyStatus);
    }

    @Transactional(readOnly = true)
    public GetActivityListResDto getActivityList(Long hobbyId, CustomUserDetails user) {
        log.info("[HobbyService] 활동 목록 조회 요청 - hobbyId={}", hobbyId);

        Hobby hobby = getHobby(hobbyId);
        User currentUser = userUtil.getCurrentUser(user);

        verifyHobbyOwner(hobby, currentUser);
        checkHobbyInProgressStatus(hobby);

        GetActivityListResDto result =
                activityRepository.getActivityList(hobby, currentUser);

        log.info("[HobbyService] 활동 목록 조회 완료 - hobbyId={}, userId={}, activityCount={}",
                hobbyId,
                currentUser.getId(),
                result.getActivities().size()
        );

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
        if (!Objects.equals(hobby.getUser(), currentUser)) {
            log.warn("[HobbyService] 권한 없음 - HobbyOwnerId: {}, CurrentUserId: {}",
                    hobby.getUser().getId(), currentUser.getId());
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

        // hobby 조회
        Hobby hobby = (hobbyId != null)
                ? getHobby(hobbyId)
                : getLatestInProgressHobby(currentUser);

        // 진행 중 취미 자체가 없는 경우
        if (hobby == null) {
            return GetStickerInfoResDto.empty();
        }

        // 권한 + 상태 체크
        verifyHobbyOwner(hobby, currentUser);
        checkHobbyInProgressStatus(hobby);

        // 기간 설정 여부
        boolean durationSet = hobby.getGoalDays() != null;

        // 오늘 기록 여부 (Redis)
        boolean recordedToday =
                redisUtil.hasKey(
                        redisUtil.createRecordKey(currentUser.getId(), hobby.getId())
                );


        // 전체 스티커 개수 (빈칸 포함)
        int totalStickerNum = hobby.getCurrentStickerNum();

        int totalSlotCount = totalStickerNum;
        if(!recordedToday) totalSlotCount++; // 오늘 기록한게 없으면 빈칸도 포함해서 현재 페이지 정하기

        // 현재 조회하고자 하는 페이지
        int currentPage = (page == null)
                ? calculateCurrentPage(totalSlotCount, size) // 현재 진행 중인 페이지가 어디인지
                : page;

        // 전체 페이지
        int totalPage = ((totalSlotCount - 1) / size) + 1;

        if(currentPage <= 0 || currentPage > totalPage) {
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

        return new GetStickerInfoResDto(durationSet, recordedToday, currentPage, totalPage, size, totalStickerNum, currentPage > 1, currentPage < totalPage, stickerDto);
    }

    private int calculateCurrentPage(int totalSlotCount, int size) {
        if (totalSlotCount <= 0) return 1;
        return ((totalSlotCount - 1) / size) + 1; // total = 10개 -> 1페이지, total = 29 (28+1) -> 2페이지
    }

    private Hobby getLatestInProgressHobby(User user) {
        return hobbyRepository
                .findTopByUserAndStatusOrderByCreatedAtDesc(
                        user,
                        HobbyStatus.IN_PROGRESS
                )
                .orElse(null);
    }



}
