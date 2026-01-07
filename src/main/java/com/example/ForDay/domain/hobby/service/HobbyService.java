package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.domain.activity.ActivityRepository;
import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.hobby.dto.request.ActivityAIRecommendReqDto;
import com.example.ForDay.domain.hobby.dto.request.ActivityCreateReqDto;
import com.example.ForDay.domain.hobby.dto.response.ActivityAIRecommendResDto;
import com.example.ForDay.domain.hobby.dto.response.ActivityCreateResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.entity.HobbyPurpose;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.ai.dto.response.AiActivityResult;
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

import java.time.LocalDate;
import java.util.List;
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

    @Transactional
    public ActivityCreateResDto hobbyCreate(ActivityCreateReqDto reqDto, CustomUserDetails user) {
        log.info("[ActivityCreate] 요청 시작 - userId={}, hobbyCardId={}",
                user.getUsername(), reqDto.getHobbyCardId());

        if (Boolean.TRUE.equals(reqDto.getIsDurationSet()) && reqDto.getGoalDays() == null) {
            log.warn("[ActivityCreate] 기간 설정 오류 - isDurationSet=true, goalDays=null, userId={}", user.getUsername());
            throw new CustomException(ErrorCode.GOAL_DAYS_REQUIRED);
        }

        if (Boolean.FALSE.equals(reqDto.getIsDurationSet()) && reqDto.getGoalDays() != null) {
            log.warn("[ActivityCreate] 기간 설정 오류 - isDurationSet=false, goalDays={}, userId={}", reqDto.getGoalDays(), user.getUsername());
            throw new CustomException(ErrorCode.GOAL_DAYS_NOT_ALLOWED);
        }

        User currentUser = userUtil.getCurrentUser(user);
        Integer executionCount = reqDto.getExecutionCount();
        Integer goalDays = reqDto.getGoalDays();

        Hobby hobby = Hobby.builder()
                .user(currentUser)
                .hobbyCardId(reqDto.getHobbyCardId())
                .hobbyName(reqDto.getHobbyName())
                .hobbyTimeMinutes(reqDto.getHobbyTimeMinutes())
                .executionCount(executionCount)
                .goalGrapes(goalDays == null ? null : calculateGoalGrapes(goalDays, executionCount))
                .goalDays(goalDays)
                .status(HobbyStatus.IN_PROGRESS)
                .startDate(LocalDate.now())
                .build();

        reqDto.getHobbyPurposes().forEach(purpose ->
                hobby.addPurpose(
                        HobbyPurpose.builder()
                                .content(purpose)
                                .build()
                )
        );

        hobbyRepository.save(hobby);
        log.info("[ActivityCreate] Hobby 생성 완료 - hobbyId={}, userId={}",
                hobby.getId(), currentUser.getId());

        activityRepository.saveAll(
                reqDto.getActivities().stream()
                .map(routine -> Activity.builder()
                        .user(currentUser)
                        .hobby(hobby)
                        .content(routine.getContent())
                        .description(routine.getDescription())
                        .build()
                )
                .toList());

        log.info("[ActivityCreate] Activity 생성 완료 - hobbyId={}, activityCount={}, userId={}",
                hobby.getId(), reqDto.getActivities().size(), currentUser.getId());

        return new ActivityCreateResDto("취미 활동이 성공적으로 생성되었습니다.", reqDto.getActivities().size(), hobby.getId());
    }

    private Integer calculateGoalGrapes(Integer goalDays, Integer executionCount) {
        if (goalDays % 7 != 0) {
            throw new CustomException(ErrorCode.GOAL_DAYS_NOT_MULTIPLE_OF_SEVEN);
        }

        int weeks = goalDays / 7;
        return weeks * executionCount;
    }

    public ActivityAIRecommendResDto activityAiRecommend(
            ActivityAIRecommendReqDto reqDto,
            CustomUserDetails user) throws Exception {
        log.info("[AI-RECOMMEND][START] user={}, hobby={}, time={}min, purposes={}, executionCount={}",
                user.getUsername(),
                reqDto.getHobbyName(),
                reqDto.getHobbyTimeMinutes(),
                reqDto.getHobbyPurposes(),
                reqDto.getExecutionCount()
        );

        int aiCallCount = aiCallCountService.increaseAndGet(user.getUsername());
        log.info("[AI-RECOMMEND][COUNT] user={}, aiCallCount={}", user.getUsername(), aiCallCount);

        log.info("[AI-RECOMMEND][CALL] user={} calling AI model", user.getUsername());


        AiActivityResult aiResult = aiActivityService.recommend(reqDto);

        if (aiResult.getActivities() == null || aiResult.getActivities().isEmpty()) {
            throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
        }


        log.info("[AI-RECOMMEND][RESULT] user={}, activitySize={}",
                user.getUsername(),
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

}
