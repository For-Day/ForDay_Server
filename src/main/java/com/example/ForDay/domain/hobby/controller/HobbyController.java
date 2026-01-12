package com.example.ForDay.domain.hobby.controller;

import com.example.ForDay.domain.hobby.dto.requ.AddActivityReqDto;
import com.example.ForDay.domain.hobby.dto.request.ActivityAIRecommendReqDto;
import com.example.ForDay.domain.hobby.dto.request.OthersActivityRecommendReqDto;
import com.example.ForDay.domain.hobby.dto.response.AddActivityResDto;
import com.example.ForDay.domain.hobby.dto.response.OthersActivityRecommendResDto;
import com.example.ForDay.domain.hobby.dto.response.ActivityAIRecommendResDto;
import com.example.ForDay.domain.hobby.dto.request.ActivityCreateReqDto;
import com.example.ForDay.domain.hobby.dto.response.ActivityCreateResDto;
import com.example.ForDay.domain.hobby.service.HobbyService;
import com.example.ForDay.global.oauth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
}
