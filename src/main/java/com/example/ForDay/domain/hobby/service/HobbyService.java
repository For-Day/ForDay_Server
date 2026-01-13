package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.domain.hobby.dto.request.ActivityCreateReqDto;
import com.example.ForDay.domain.hobby.dto.request.AddActivityReqDto;
import com.example.ForDay.domain.hobby.dto.response.*;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.entity.HobbyCard;
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
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class HobbyService {
    @Value("${ai.max-call-limit}")
    private int maxCallLimit;

    private final HobbyRepository hobbyRepository;
    private final UserUtil userUtil;
    private final ActivityRepository activityRepository;
    private final AiActivityService aiActivityService;
    private final AiCallCountService aiCallCountService;
    private final HobbyCardRepository hobbyCardRepository;

    @Transactional
    public ActivityCreateResDto hobbyCreate(ActivityCreateReqDto reqDto, CustomUserDetails user) {
        log.info("[ActivityCreate] 요청 시작 - userId={}, hobbyCardId={}",
                user.getUsername(), reqDto.getHobbyCardId());

        User currentUser = userUtil.getCurrentUser(user);

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


        return new ActivityCreateResDto("취미가 성공적으로 생성되었습니다.", hobby.getId());
    }

    @Transactional
    public ActivityAIRecommendResDto activityAiRecommend(Long hobbyId, CustomUserDetails user) throws Exception {
        User currentUser = userUtil.getCurrentUser(user);
        String userId = currentUser.getId();
        Hobby hobby = getHobby(hobbyId);
        verifyHobbyOwner(hobby, currentUser);

        log.info("[AI-RECOMMEND][START] user={}: ",
                userId
        );

        int aiCallCount = aiCallCountService.increaseAndGet(userId, hobbyId);
        log.info("[AI-RECOMMEND][COUNT] user={}, aiCallCount={}", userId, aiCallCount);

        log.info("[AI-RECOMMEND][CALL] user={} calling AI model", userId);


        AiActivityResult aiResult = aiActivityService.activityRecommend(hobby);

        if (aiResult.getActivities() == null || aiResult.getActivities().isEmpty()) {
            throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
        }


        log.info("[AI-RECOMMEND][RESULT] user={}, activitySize={}",
                userId,
                aiResult.getActivities().size()
        );

        List<ActivityAIRecommendResDto.ActivityDto> activities =
                IntStream.range(0, aiResult.getActivities().size())
                        .mapToObj(i -> {
                            AiActivityResult.ActivityCard a =
                                    aiResult.getActivities().get(i);

                            return new ActivityAIRecommendResDto.ActivityDto(
                                    (long) (i + 1),
                                    a.getTopic(),
                                    a.getContent(),
                                    a.getDescription()
                            );
                        })
                        .toList();

        log.info("[AI-RECOMMEND][SUCCESS] user={}, returnedActivities={}",
                user.getUsername(),
                activities.size()
        );

        return new ActivityAIRecommendResDto(
                "AI가 취미 활동을 추천했습니다.",
                aiCallCount,
                maxCallLimit,
                activities
        );
    }

    @Transactional
    public OthersActivityRecommendResDto othersActivityRecommendV1(Long hobbyId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        Hobby hobby = getHobby(hobbyId);
        verifyHobbyOwner(hobby, currentUser);

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

    public GetHobbyActivitiesResDto getHobbyActivities(Long hobbyId, CustomUserDetails user) {
        Hobby hobby = getHobby(hobbyId);
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[GetHobbyActivities] 조회 시작 - UserId: {}, HobbyId: {}", currentUser.getId(), hobbyId);

        // 현재 사용자가 hobby의 소유자인지 판별
        verifyHobbyOwner(hobby, currentUser);

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

    public GetHomeHobbyInfoResDto getHomeHobbyInfo(Long hobbyId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[GetHomeHobbyInfo] 대시보드 조회 - UserId: {}, TargetHobbyId: {}",
                currentUser.getId(), hobbyId == null ? "DEFAULT(Latest)" : hobbyId);

        return hobbyRepository.getHomeHobbyInfo(hobbyId, currentUser);
    }

    public MyHobbySettingResDto myHobbySetting(CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        log.info("[MyHobbySetting] 취미 설정 목록 조회 - UserId: {}", currentUser.getId());

        return hobbyRepository.myHobbySetting(currentUser);
    }
}
