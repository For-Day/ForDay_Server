package com.example.ForDay.domain.hobby.controller;

import com.example.ForDay.domain.hobby.dto.request.AddActivityReqDto;
import com.example.ForDay.domain.hobby.dto.request.ActivityAIRecommendReqDto;
import com.example.ForDay.domain.hobby.dto.request.OthersActivityRecommendReqDto;
import com.example.ForDay.domain.hobby.dto.response.*;
import com.example.ForDay.domain.hobby.dto.request.ActivityCreateReqDto;
import com.example.ForDay.domain.hobby.service.HobbyService;
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

    @Override
    @PostMapping("/create")
    public ActivityCreateResDto hobbyCreate(@RequestBody @Valid ActivityCreateReqDto reqDto,
                                            @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.hobbyCreate(reqDto, user);
    }

    @Override
    @PostMapping("/activities/ai/recommend")
    public ActivityAIRecommendResDto activityAiRecommend(@RequestBody @Valid ActivityAIRecommendReqDto reqDto,
                                                         @AuthenticationPrincipal CustomUserDetails user) throws Exception {
        return hobbyService.activityAiRecommend(reqDto, user);
    }

    @Override
    @PostMapping("/activities/others/v1")
    public OthersActivityRecommendResDto othersActivityRecommendV1(@RequestBody @Valid OthersActivityRecommendReqDto reqDto) {
        return hobbyService.othersActivityRecommendV1(reqDto);
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
}
