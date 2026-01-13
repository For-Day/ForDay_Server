package com.example.ForDay.domain.hobby.controller;

import com.example.ForDay.domain.hobby.dto.request.AddActivityReqDto;
import com.example.ForDay.domain.hobby.dto.request.ActivityAIRecommendReqDto;
import com.example.ForDay.domain.hobby.dto.request.ActivityCreateReqDto;
import com.example.ForDay.domain.hobby.dto.request.OthersActivityRecommendReqDto;
import com.example.ForDay.domain.hobby.dto.response.ActivityAIRecommendResDto;
import com.example.ForDay.domain.hobby.dto.response.ActivityCreateResDto;
import com.example.ForDay.domain.hobby.dto.response.AddActivityResDto;
import com.example.ForDay.domain.hobby.dto.response.OthersActivityRecommendResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Hobby", description = "취미 및 활동 관련 API")
public interface HobbyControllerDocs {

    @Operation(
            summary = "취미 루틴 생성",
            description = "사용자의 취미 정보로 루틴을 생성합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "취미 루틴 생성 성공",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 200, \"success\": true, \"data\": {\"message\": \"취미 활동이 성공적으로 생성되었습니다.\", \"createdActivityCount\": 3, \"hobbyId\": 1}}"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "입력값 유효성 검사 실패",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"VALIDATION_ERROR\", \"message\": \"{hobbyName=hobbyName은 필수입니다.}\"}}"))
            )
    })
    ActivityCreateResDto hobbyCreate(ActivityCreateReqDto reqDto, CustomUserDetails user);

    @Operation(
            summary = "AI 기반 취미 활동 추천",
            description = "사용자의 취미, 가용 시간, 목적을 바탕으로 AI가 맞춤형 활동 리스트를 추천합니다. 일일 3회 제한이 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "추천 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "AI 호출 횟수 초과",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"AI_CALL_LIMIT_EXCEEDED\", \"message\": \"AI 최대 호출 횟수를 초과하였습니다.\"}}"))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "AI 응답 파싱 실패",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 422, \"success\": false, \"data\": {\"errorClassName\": \"AI_RESPONSE_INVALID\", \"message\": \"AI 응답 형식이 올바르지 않아 데이터를 처리할 수 없습니다.\"}}"))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "AI 서비스 연결 오류",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 502, \"success\": false, \"data\": {\"errorClassName\": \"AI_SERVICE_ERROR\", \"message\": \"AI 서비스 연결 중에 오류가 발생했습니다. 잠시 후 다시 시도해주세요.\"}}"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "입력값 유효성 검사 실패",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"VALIDATION_ERROR\", \"message\": \"{hobbyName=hobbyName은 필수입니다.}\"}}"))
            )
    })
    ActivityAIRecommendResDto activityAiRecommend(ActivityAIRecommendReqDto reqDto, CustomUserDetails user) throws Exception;

    @Operation(
            summary = "다른 포비들의 활동 조회 (AI 기반)",
            description = "초기 유저 데이터가 없을 때, AI가 비슷한 조건의 다른 유저들이 할 법한 인기 활동 3개를 생성하여 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "AI 호출 횟수 초과",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"AI_CALL_LIMIT_EXCEEDED\", \"message\": \"AI 최대 호출 횟수를 초과하였습니다.\"}}"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "취미 카드 조회 실패",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"HOBBY_CARD_NOT_FOUND\", \"message\": \"존재하지 않는 취미 카드입니다.\"}}"))
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "AI 응답 형식 오류",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 422, \"success\": false, \"data\": {\"errorClassName\": \"AI_RESPONSE_INVALID\", \"message\": \"AI 응답 형식이 올바르지 않아 데이터를 처리할 수 없습니다.\"}}"))
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "AI 서비스 통신 실패",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 502, \"success\": false, \"data\": {\"errorClassName\": \"AI_SERVICE_ERROR\", \"message\": \"AI 서비스 연결 중에 오류가 발생했습니다. 잠시 후 다시 시도해주세요.\"}}"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "입력값 유효성 검사 실패",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"VALIDATION_ERROR\", \"message\": \"{hobbyName=hobbyName은 필수입니다.}\"}}"))
            )
    })
    OthersActivityRecommendResDto othersActivityRecommendV1(OthersActivityRecommendReqDto reqDto);



    @Operation(
            summary = "취미 활동 추가",
            description = """
                특정 취미(hobby)에 대해 여러 개의 활동(Activity)을 한 번에 생성합니다.
                
                - AI 추천 활동 / 사용자 직접 입력 활동 모두 처리
                - 생성된 활동 개수를 반환합니다.
                """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "취미 활동 생성 성공",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "성공 응답",
                                    value = """
                                {
                                  "status": 200,
                                  "success": true,
                                  "data": {
                                    "message": "취미 활동이 정상적으로 생성되었습니다.",
                                    "createdActivityNum": 3
                                  }
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "취미를 찾을 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "취미 없음",
                                    value = """
                                {
                                  "status": 404,
                                  "success": false,
                                  "data": {
                                    "errorClassName": "HOBBY_NOT_FOUND",
                                    "message": "존재하지 않는 취미입니다."
                                  }
                                }
                                """
                            )
                    )
            )
    })
    AddActivityResDto addActivity(@PathVariable(value = "hobbyId") Long hobbyId, @RequestBody @Valid AddActivityReqDto reqDto, @AuthenticationPrincipal CustomUserDetails user);
}
