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

    @GetMapping("/activities/ai/recommend/test")
    public ActivityAIRecommendResDto testActivityAiRecommend(@RequestParam(name = "hobbyId") Long hobbyId,
                                                             @AuthenticationPrincipal CustomUserDetails user) throws Exception {
        return hobbyService.testActivityAiRecommend(hobbyId, user);
    }

    @Override
    @GetMapping("/activities/others/v1")
    public OthersActivityRecommendResDto othersActivityRecommendV1(@RequestParam(name = "hobbyId") Long hobbyId,
                                                                   @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.othersActivityRecommendV1(hobbyId, user);
    }

    @Override
    @PostMapping("/{hobbyId}/activities")
    public AddActivityResDto addActivity(@PathVariable(value = "hobbyId") Long hobbyId,
                                         @RequestBody @Valid AddActivityReqDto reqDto,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.addActivity(hobbyId, reqDto, user);
    }

    @Override
    @GetMapping("/{hobbyId}/activities")
    public GetHobbyActivitiesResDto getHobbyActivities(@PathVariable(value = "hobbyId") Long hobbyId,
                                                       @AuthenticationPrincipal CustomUserDetails user,
                                                       @RequestParam(name = "size", required = false) Integer size) {
        return hobbyService.getHobbyActivities(hobbyId, user, size);
    }

    @Override
    @PostMapping("/activities/{activityId}/record")
    public RecordActivityResDto recordActivity(@PathVariable(value = "activityId") Long activityId,
                                               @RequestBody @Valid RecordActivityReqDto reqDto,
                                               @AuthenticationPrincipal CustomUserDetails user) {
        return activityService.recordActivity(activityId, reqDto, user);
    }

    @PostMapping("/activities/{activityId}/record/test")
    public RecordActivityResDto testRecordActivity(@PathVariable(value = "activityId") Long activityId,
                                                   @RequestBody @Valid RecordActivityReqDto reqDto,
                                                   @AuthenticationPrincipal CustomUserDetails user) {
        return activityService.testRecordActivity(activityId, reqDto, user);
    }

    @Override
    @GetMapping("/home")
    public GetHomeHobbyInfoResDto getHomeHobbyInfo(@RequestParam(value = "hobbyId", required = false) Long hobbyId,
                                                   @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.getHomeHobbyInfo(hobbyId, user);
    }

    @Override
    @GetMapping("/setting")
    public MyHobbySettingResDto myHobbySetting(@AuthenticationPrincipal CustomUserDetails user,
                                               @RequestParam(name = "hobbyStatus", defaultValue = "IN_PROGRESS") HobbyStatus hobbyStatus) {
        return hobbyService.myHobbySetting(user, hobbyStatus);
    }

    @Override
    @GetMapping("/{hobbyId}/activities/list")
    public GetActivityListResDto getActivityList(@PathVariable(value = "hobbyId") Long hobbyId,
                                                 @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.getActivityList(hobbyId, user);
    }

    @Override
    @PatchMapping("/{hobbyId}/time")
    public MessageResDto updateHobbyTime(
            @PathVariable Long hobbyId,
            @RequestBody @Valid HobbyTimePayload reqDto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return hobbyService.updateHobbyTime(hobbyId, reqDto, user);
    }

    @Override
    @PatchMapping("/{hobbyId}/execution-count")
    public MessageResDto updateExecutionCount(
            @PathVariable Long hobbyId,
            @RequestBody @Valid ExecutionCountPayload reqDto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return hobbyService.updateExecutionCount(hobbyId, reqDto, user);
    }

    @Override
    @PatchMapping("/{hobbyId}/goal-days")
    public MessageResDto updateGoalDays(
            @PathVariable Long hobbyId,
            @RequestBody @Valid GoalDaysPayload reqDto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return hobbyService.updateGoalDays(hobbyId, reqDto, user);
    }

    @Override
    @PatchMapping("/{hobbyId}/status")
    public MessageResDto updateHobbyStatus(@PathVariable(value = "hobbyId") Long hobbyId,
                                           @RequestBody @Valid UpdateHobbyStatusReqDto reqDto,
                                           @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.updateHobbyStatus(hobbyId, reqDto, user);
    }

    @Override
    @PatchMapping("/{hobbyId}/extension")
    public SetHobbyExtensionResDto setHobbyExtension(@PathVariable(value = "hobbyId") Long hobbyId,
                                                     @RequestBody @Valid SetHobbyExtensionReqDto reqDto,
                                                     @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.setHobbyExtension(hobbyId, reqDto, user);
    }

    @Override
    @GetMapping("/stickers")
    public GetStickerInfoResDto getStickerInfo(@RequestParam(value = "hobbyId", required = false) Long hobbyId,
                                               @RequestParam(value = "page", required = false) Integer page,
                                               @RequestParam(value = "size", required = false, defaultValue = "28") Integer size,
                                               @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.getStickerInfo(hobbyId, page, size, user);
    }

    @Override
    @PatchMapping("/cover-image")
    public SetHobbyCoverImageResDto setHobbyCoverImage(@RequestBody @Valid SetHobbyCoverImageReqDto reqDto,
                                                       @AuthenticationPrincipal CustomUserDetails user) throws Exception {
        return hobbyService.setHobbyCoverImage(reqDto, user);
    }

    @Override
    @PostMapping("/{hobbyId}/activities/{activityId}/collect")
    public CollectActivityResDto collectActivity(@PathVariable(name = "hobbyId") Long hobbyId,
                                                 @PathVariable(name = "activityId") Long activityId,
                                                 @AuthenticationPrincipal CustomUserDetails user) {
        return activityService.collectActivity(hobbyId, activityId, user);
    }

    @Override
    @GetMapping("/stories/tabs")
    public GetHobbyStoryTabsResDto getHobbyStoryTabs(@AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.getHobbyStoryTabs(user);
    }

    @Override
    @GetMapping("/check")
    public CanCreateHobbyResDto canCreateHobby(@RequestParam(value = "name") String name,
                                               @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.canCreateHobby(name, user);
    }

    @Override
    @GetMapping("/info/re-check")
    public ReCheckHobbyInfoResDto reCheckHobbyInfo(@AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.reCheckHobbyInfo(user);
    }

    @PutMapping("/{hobbyId}/update")
    public UpdateHobbyResDto updateHobby(@PathVariable(name = "hobbyId") Long hobbyId,
                                         @RequestBody @Valid UpdateHobbyReqDto reqDto,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.updateHobby(hobbyId, reqDto, user);
    }
}
