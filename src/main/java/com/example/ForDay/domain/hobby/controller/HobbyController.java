package com.example.ForDay.domain.hobby.controller;

import com.example.ForDay.domain.activity.service.ActivityService;
import com.example.ForDay.domain.hobby.dto.request.*;
import com.example.ForDay.domain.hobby.dto.response.*;
import com.example.ForDay.domain.hobby.service.HobbyService;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hobbies")
public class HobbyController implements HobbyControllerDocs {
    private final HobbyService hobbyService;
    private final ActivityService activityService;

    @Override
    @PostMapping("/create")
    public ActivityCreateResDto hobbyCreate(@RequestBody @Valid ActivityCreateReqDto reqDto,
                                            @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.hobbyCreate(reqDto, user);
    }

    @Override
    @GetMapping("/activities/ai/recommend")
    public ActivityAIRecommendResDto activityAiRecommend(@RequestParam(name = "hobbyId") Long hobbyId,
                                                         @AuthenticationPrincipal CustomUserDetails user) throws Exception {
        return hobbyService.activityAiRecommend(hobbyId, user);
    }

    @Override
    @GetMapping("/activities/others/v1")
    public OthersActivityRecommendResDto othersActivityRecommendV1(@RequestParam(name = "hobbyId") Long hobbyId,  @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.othersActivityRecommendV1(hobbyId, user);
    }

    @Override
    @PostMapping("/{hobbyId}/activities")
    public AddActivityResDto addActivity(@PathVariable(value = "hobbyId") Long hobbyId, @RequestBody @Valid AddActivityReqDto reqDto, @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.addActivity(hobbyId, reqDto, user);
    }

    @Override
    @GetMapping("/{hobbyId}/activities")
    public GetHobbyActivitiesResDto getHobbyActivities(@PathVariable(value = "hobbyId") Long hobbyId, @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.getHobbyActivities(hobbyId, user);
    }

    @Override
    @PostMapping("/activities/{activityId}/record")
    public RecordActivityResDto recordActivity(@PathVariable(value = "activityId") Long activityId, @RequestBody @Valid RecordActivityReqDto reqDto, @AuthenticationPrincipal CustomUserDetails user) {
        return activityService.recordActivity(activityId, reqDto, user);
    }

    @Override
    @GetMapping("/home")
    public GetHomeHobbyInfoResDto getHomeHobbyInfo(@RequestParam(value = "hobbyId", required = false) Long hobbyId, @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.getHomeHobbyInfo(hobbyId, user);
    }

    @Override
    @GetMapping("/setting")
    public MyHobbySettingResDto myHobbySetting(@AuthenticationPrincipal CustomUserDetails user, @RequestParam(name = "hobbyStatus", defaultValue = "IN_PROGRESS") HobbyStatus hobbyStatus) {
        return hobbyService.myHobbySetting(user, hobbyStatus);
    }

    @GetMapping("/{hobbyId}/activities/list")
    public GetActivityListResDto getActivityList(@PathVariable(value = "hobbyId") Long hobbyId, @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.getActivityList(hobbyId, user);
    }

    @PatchMapping("/{hobbyId}")
    public MessageResDto updateHobbyInfo(@PathVariable(value = "hobbyId") Long hobbyId, @RequestBody @Valid UpdateHobbyInfoReqDto reqDto, @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.updateHobbyInfo(hobbyId, reqDto, user);
    }

    @PatchMapping("/{hobbyId}/status")
    public MessageResDto updateHobbyStatus(@PathVariable(value = "hobbyId") Long hobbyId, @RequestBody @Valid UpdateHobbyStatusReqDto reqDto, @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.updateHobbyStatus(hobbyId, reqDto, user);
    }
}
