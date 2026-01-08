package com.example.ForDay.domain.hobby.controller;

import com.example.ForDay.domain.hobby.dto.request.ActivityAIRecommendReqDto;
import com.example.ForDay.domain.hobby.dto.request.OthersActivityRecommendReqDto;
import com.example.ForDay.domain.hobby.dto.response.OthersActivityRecommendResDto;
import com.example.ForDay.domain.hobby.dto.response.ActivityAIRecommendResDto;
import com.example.ForDay.domain.hobby.dto.request.ActivityCreateReqDto;
import com.example.ForDay.domain.hobby.dto.response.ActivityCreateResDto;
import com.example.ForDay.domain.hobby.service.HobbyService;
import com.example.ForDay.global.oauth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hobbies")
public class HobbyController {
    private final HobbyService hobbyService;

    @PostMapping("/activities/create")
    @Operation(
            summary = "취미 루틴 생성",
            description = "사용자의 취미 정보로 루틴을 생성합니다."
    )
    public ActivityCreateResDto hobbyCreate(@RequestBody @Valid ActivityCreateReqDto reqDto, @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.hobbyCreate(reqDto, user);
    }

    @Operation(
            summary = "AI 기반 취미 활동 추천",
            description = "사용자의 취미, 가용 시간, 목적을 바탕으로 AI가 맞춤형 활동 리스트를 추천합니다. 일일 3회 제한이 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "추천 성공"),
            @ApiResponse(responseCode = "400", description = "AI 호출 횟수 초과 (DAILY_LIMIT_EXCEEDED)"),
            @ApiResponse(responseCode = "422", description = "AI 응답 파싱 실패 (AI_RESPONSE_INVALID)")
    })
    @PostMapping("/activities/ai/recommend")
    public ActivityAIRecommendResDto activityAiRecommend(@RequestBody @Valid ActivityAIRecommendReqDto reqDto, @AuthenticationPrincipal CustomUserDetails user) throws Exception {
        return hobbyService.activityAiRecommend(reqDto, user);
    }

    @Operation(
            summary = "다른 포비들의 활동 조회 (AI 기반)",
            description = "초기 유저 데이터가 없을 때, AI가 비슷한 조건의 다른 유저들이 할 법한 인기 활동 3개를 생성하여 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "HobbyCard를 찾을 수 없음 (HOBBY_CARD_NOT_FOUND)"),
            @ApiResponse(responseCode = "422", description = "AI 응답 형식이 올바르지 않음 (AI_RESPONSE_INVALID)"),
            @ApiResponse(responseCode = "502", description = "AI 서비스 통신 실패 (AI_SERVICE_ERROR)")
    })
    @PostMapping("/activities/others/v1")
    public OthersActivityRecommendResDto othersActivityRecommendV1(@RequestBody @Valid OthersActivityRecommendReqDto reqDto) {
        return hobbyService.othersActivityRecommendV1(reqDto);
    }
}
