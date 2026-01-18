package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.dto.request.*;
import com.example.ForDay.domain.hobby.dto.response.*;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyCardRepository;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.ai.dto.response.AiActivityResult;
import com.example.ForDay.global.ai.dto.response.AiOthersActivityResult;
import com.example.ForDay.global.ai.service.AiActivityService;
import com.example.ForDay.global.ai.service.AiCallCountService;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class HobbyService {
    private static final Integer DEFAULT_GOAL_DAYS = 66;

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

    @Transactional
    public ActivityCreateResDto hobbyCreate(ActivityCreateReqDto reqDto, CustomUserDetails user) {
        log.info("[ActivityCreate] 요청 시작 - userId={}, hobbyCardId={}",
                user.getUsername(), reqDto.getHobbyCardId());

        User currentUser = userUtil.getCurrentUser(user);

        // 이미 진행 중인 취미가 두개인지 검사
        long hobbyCount = hobbyRepository.countByStatusAndUser(HobbyStatus.IN_PROGRESS, currentUser);
        if(hobbyCount >= 2) {
            throw new CustomException(ErrorCode.MAX_IN_PROGRESS_HOBBY_EXCEEDED);
        }

        Hobby hobby = Hobby.builder()
                .user(currentUser)
                .hobbyCardId(reqDto.getHobbyCardId())
                .hobbyName(reqDto.getHobbyName())
                .hobbyPurpose(reqDto.getHobbyPurpose())
                .hobbyTimeMinutes(reqDto.getHobbyTimeMinutes())
                .executionCount(reqDto.getExecutionCount())
                .goalDays(reqDto.getIsDurationSet() ? 66 : null)
                .status(HobbyStatus.IN_PROGRESS)
                .build();

        hobbyRepository.save(hobby);
        log.info("[ActivityCreate] Hobby 생성 완료 - hobbyId={}, userId={}",
                hobby.getId(), currentUser.getId());

        // 온보딩이 완료되지 않은 경우에만 완료로 전환되도록 설정
        if(!currentUser.isOnboardingCompleted()) {
            currentUser.completeOnboarding();
        }

        return new ActivityCreateResDto("취미가 성공적으로 생성되었습니다.", hobby.getId());
    }

    @Transactional(readOnly = true)
    public ActivityAIRecommendResDto activityAiRecommend(
            Long hobbyId,
            CustomUserDetails user
    ) {
        User currentUser = userUtil.getCurrentUser(user);
        String userId = currentUser.getId();

        Hobby hobby = getHobby(hobbyId);
        verifyHobbyOwner(hobby, currentUser);
        checkHobbyInProgressStatus(hobby); // 현재 진행 중인 취미에 대해서만 ai 추천 가능

        // 사전 차단 (UX)
        int currentCount = aiCallCountService.increaseAndGet(currentUser.getSocialId(), hobbyId);

        log.info("[AI-RECOMMEND][CALL] user={} calling AI model", userId);


        // 2. FastAPI 요청 객체 생성
        FastAPIRecommendReq requestDto = FastAPIRecommendReq.builder()
                .userId(currentUser.getId())
                .userHobbyId(hobby.getId().intValue())
                .hobbyName(hobby.getHobbyName())
                .hobbyPurpose(hobby.getHobbyPurpose())
                .hobbyTimeMinutes(hobby.getHobbyTimeMinutes())
                .executionCount(hobby.getExecutionCount())
                .goalDays(hobby.getGoalDays())
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
        verifyHobbyOwner(hobby, currentUser);
        checkHobbyInProgressStatus(hobby);

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


    private Hobby getHobby(Long hobbyId) {
        return hobbyRepository.findById(hobbyId).orElseThrow(() -> new CustomException(ErrorCode.HOBBY_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public GetHobbyActivitiesResDto getHobbyActivities(Long hobbyId, CustomUserDetails user) {
        Hobby hobby = getHobby(hobbyId);
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[GetHobbyActivities] 조회 시작 - UserId: {}, HobbyId: {}", currentUser.getId(), hobbyId);

        // 현재 사용자가 hobby의 소유자인지 판별
        verifyHobbyOwner(hobby, currentUser);
        checkHobbyInProgressStatus(hobby);

        GetHobbyActivitiesResDto response = activityRepository.getHobbyActivities(hobby);
        log.info("[GetHobbyActivities] 조회 완료 - 활동 개수: {}", response.getActivities().size());
        return response; // 해당 취미에 대한 활동 목록 조회

    }

    private void verifyHobbyOwner(Hobby hobby, User currentUser) {
        if (!Objects.equals(hobby.getUser(), currentUser)) {
            log.warn("[HobbyService] 권한 없음 - HobbyOwnerId: {}, CurrentUserId: {}",
                    hobby.getUser().getId(), currentUser.getId());
            throw new CustomException(ErrorCode.NOT_HOBBY_OWNER);
        }
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


    private Hobby checkHobbyUpdateable(Long hobbyId, CustomUserDetails user) {
        Hobby hobby = getHobby(hobbyId);
        User currentUser = userUtil.getCurrentUser(user);

        verifyHobbyOwner(hobby, currentUser);

        if (!hobby.isUpdatable()) {
            throw new CustomException(ErrorCode.INVALID_HOBBY_STATUS);
        }
        return hobby;
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

        HobbyStatus currentStatus = hobby.getStatus();
        HobbyStatus targetStatus = reqDto.getHobbyStatus();

        // 동일 상태 요청
        if (currentStatus == targetStatus) {
            log.info("[HobbyService] 취미 상태 변경 요청 무시 (동일 상태) - hobbyId={}, status={}",
                    hobbyId, currentStatus);
            return new MessageResDto("이미 해당 상태입니다.");
        }

        switch (targetStatus) {
            case IN_PROGRESS -> {
                long inProgressCount =
                        hobbyRepository.countByStatusAndUser(
                                HobbyStatus.IN_PROGRESS,
                                currentUser
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


    private void checkHobbyInProgressStatus(Hobby hobby) {
        if(!hobby.getStatus().equals(HobbyStatus.IN_PROGRESS)) {
            throw new CustomException(ErrorCode.INVALID_HOBBY_STATUS);
        }
    }

}
