package com.example.ForDay.domain.hobby.controller;

import com.example.ForDay.domain.hobby.dto.request.*;
import com.example.ForDay.domain.hobby.dto.response.*;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Hobby", description = "취미 및 활동 관련 API")
public interface HobbyControllerDocs {

    @Operation(
            summary = "취미 생성",
            description = "사용자의 취미를 생성합니다."
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
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "진행 중인 취미 개수 초과",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "MAX_IN_PROGRESS_HOBBY_EXCEEDED",
                                    value = """
                                            {
                                              "status": 400,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "MAX_IN_PROGRESS_HOBBY_EXCEEDED",
                                                "message": "이미 진행 중인 취미는 최대 2개까지 등록할 수 있습니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "중복 취미 요청 (닉네임 미설정 포함)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(example = "{\n" +
                                    "  \"status\": 409,\n" +
                                    "  \"success\": false,\n" +
                                    "  \"data\": {\n" +
                                    "    \"errorClassName\": \"DUPLICATE_HOBBY_REQUEST\",\n" +
                                    "    \"message\": \"같은 취미에 대한 중복 요청입니다. (닉네임 설정을 완료하세요.)\"\n" +
                                    "  }\n" +
                                    "}"))
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
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "취미 소유자가 아님",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "NOT_HOBBY_OWNER",
                                    value = """
                                            {
                                              "status": 403,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "NOT_HOBBY_OWNER",
                                                "message": "취미 소유자가 아닙니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "취미 조회 실패",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "HOBBY_NOT_FOUND",
                                    value = """
                                            {
                                              "status": 404,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "HOBBY_CARD_NOT_FOUND",
                                                "message": "존재하지 않는 취미입니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "현재 취미 상태에서는 작업을 수행할 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "INVALID_HOBBY_STATUS",
                                    summary = "유효하지 않은 취미 상태",
                                    value = """
                                            {
                                              "status": 400,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "INVALID_HOBBY_STATUS",
                                                "message": "현재 취미 상태에서는 해당 작업을 수행할 수 없습니다."
                                              }
                                            }
                                            """
                            )
                    )
            )

    })
    @GetMapping("/activities/ai/recommend")
    ActivityAIRecommendResDto activityAiRecommend(@RequestParam(name = "hobbyId") Long hobbyId,
                                                  @AuthenticationPrincipal CustomUserDetails user) throws Exception;

    @Operation(
            summary = "다른 포비들의 활동 조회 (AI 기반)",
            description = "초기 유저 데이터가 없을 때, AI가 비슷한 조건의 다른 유저들이 할 법한 인기 활동 3개를 생성하여 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "400",
                    description = "요청값 유효성 검사 실패",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "VALIDATION_ERROR",
                                    value = """
                                            {
                                              "status": 400,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "VALIDATION_ERROR",
                                                "message": "{hobbyName=hobbyName은 필수입니다.}"
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "AI 호출 횟수 초과",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "AI_CALL_LIMIT_EXCEEDED",
                                    value = """
                                            {
                                              "status": 400,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "AI_CALL_LIMIT_EXCEEDED",
                                                "message": "AI 최대 호출 횟수를 초과하였습니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "취미 소유자가 아님",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "NOT_HOBBY_OWNER",
                                    value = """
                                            {
                                              "status": 403,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "NOT_HOBBY_OWNER",
                                                "message": "취미 소유자가 아닙니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "취미 카드 조회 실패",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "HOBBY_CARD_NOT_FOUND",
                                    value = """
                                            {
                                              "status": 404,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "HOBBY_CARD_NOT_FOUND",
                                                "message": "존재하지 않는 취미 카드입니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "취미 조회 실패",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "HOBBY_NOT_FOUND",
                                    value = """
                                            {
                                              "status": 404,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "HOBBY_CARD_NOT_FOUND",
                                                "message": "존재하지 않는 취미입니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "422",
                    description = "AI 응답 형식 오류",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "AI_RESPONSE_INVALID",
                                    value = """
                                            {
                                              "status": 422,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "AI_RESPONSE_INVALID",
                                                "message": "AI 응답 형식이 올바르지 않아 데이터를 처리할 수 없습니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "502",
                    description = "AI 서비스 통신 실패",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "AI_SERVICE_ERROR",
                                    value = """
                                            {
                                              "status": 502,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "AI_SERVICE_ERROR",
                                                "message": "AI 서비스 연결 중에 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "현재 취미 상태에서는 작업을 수행할 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "INVALID_HOBBY_STATUS",
                                    summary = "유효하지 않은 취미 상태",
                                    value = """
                                            {
                                              "status": 400,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "INVALID_HOBBY_STATUS",
                                                "message": "현재 취미 상태에서는 해당 작업을 수행할 수 없습니다."
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    @GetMapping("/activities/others/v1")
    OthersActivityRecommendResDto othersActivityRecommendV1(@RequestParam(name = "hobbyId") Long hobbyId, CustomUserDetails userDetails);

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
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "현재 취미 상태에서는 작업을 수행할 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "INVALID_HOBBY_STATUS",
                                    summary = "유효하지 않은 취미 상태",
                                    value = """
                                            {
                                              "status": 400,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "INVALID_HOBBY_STATUS",
                                                "message": "현재 취미 상태에서는 해당 작업을 수행할 수 없습니다."
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    AddActivityResDto addActivity(@PathVariable(value = "hobbyId") Long hobbyId, @RequestBody @Valid AddActivityReqDto reqDto, @AuthenticationPrincipal CustomUserDetails user);

    @Operation(
            summary = "특정 취미의 활동 목록 조회",
            description = "특정 취미 카드에 속한 활동들을 기록 여부, 스티커 개수, 가나다 순으로 정렬하여 조회합니다. \n  홈 화면에서 나의 취미 활동 목록 조회 시 사용\n" +
                    " 오늘의 활동 기록 시 취미에 대한 취미 활동 목록 조회 시 사용"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetHobbyActivitiesResDto.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "접근 권한 없음",
                    content = @Content(examples = @ExampleObject(value = "{\n  \"status\": 403,\n  \"success\": false,\n  \"data\": {\n    \"errorClassName\": \"NOT_HOBBY_OWNER\",\n    \"message\": \"취미 소유자가 아닙니다.\"\n  }\n}"))
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
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "현재 취미 상태에서는 작업을 수행할 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "INVALID_HOBBY_STATUS",
                                    summary = "유효하지 않은 취미 상태",
                                    value = """
                                            {
                                              "status": 400,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "INVALID_HOBBY_STATUS",
                                                "message": "현재 취미 상태에서는 해당 작업을 수행할 수 없습니다."
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    GetHobbyActivitiesResDto getHobbyActivities(
            @Parameter(description = "취미 ID", example = "1") @PathVariable(value = "hobbyId") Long hobbyId,
            @AuthenticationPrincipal CustomUserDetails user, Integer size);


    @Operation(
            summary = "취미 활동 기록하기",
            description = "특정 활동에 대한 기록(스티커, 메모, 이미지)을 작성합니다. <br>" +
                    "**제약사항:** <br>" +
                    "1. 해당 취미 카드의 소유자만 작성 가능합니다. <br>" +
                    "2. 특정 취미에 대해 **하루에 단 한 번만** 기록할 수 있습니다. (Redis 체크) <br>" +
                    "3. 이미지는 최대 3개까지 등록 가능하며, S3에 실제 업로드된 상태여야 합니다."
    )
    @ApiResponses(value = {

            @ApiResponse(
                    responseCode = "200",
                    description = "기록 작성 성공",
                    content = @Content(schema = @Schema(implementation = RecordActivityResDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "중복 기록 시도",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "ALREADY_RECORDED_TODAY",
                                    value = """
                                            {
                                              "status": 400,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "ALREADY_RECORDED_TODAY",
                                                "message": "오늘 해당 취미에 대한 활동 기록을 이미 작성하였습니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "403",
                    description = "활동 소유자 아님",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "NOT_ACTIVITY_OWNER",
                                    value = """
                                            {
                                              "status": 403,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "NOT_ACTIVITY_OWNER",
                                                "message": "활동 소유자가 아닙니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "404",
                    description = "활동 없음",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "ACTIVITY_NOT_FOUND",
                                    value = """
                                            {
                                              "status": 404,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "ACTIVITY_NOT_FOUND",
                                                "message": "존재하지 않는 활동입니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "404",
                    description = "S3 이미지 없음",
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "S3_IMAGE_NOT_FOUND",
                                    value = """
                                            {
                                              "status": 404,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "S3_IMAGE_NOT_FOUND",
                                                "message": "S3에 해당 이미지가 존재하지 않습니다. 업로드 여부를 확인해주세요."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "현재 취미 상태에서는 작업을 수행할 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "INVALID_HOBBY_STATUS",
                                    summary = "유효하지 않은 취미 상태",
                                    value = """
                                            {
                                              "status": 400,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "INVALID_HOBBY_STATUS",
                                                "message": "현재 취미 상태에서는 해당 작업을 수행할 수 없습니다."
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    RecordActivityResDto recordActivity(@PathVariable(value = "activityId") Long activityId, @RequestBody @Valid RecordActivityReqDto reqDto, @AuthenticationPrincipal CustomUserDetails user);

    @Operation(
            summary = "홈 대시보드 정보 조회",
            description = "홈 화면에 필요한 취미 리스트, 활동 미리보기, 오늘 기록 여부 등을 조회합니다. <br>" +
                    "- **hobbyId가 없을 경우**: 가장 최근에 생성된(IN_PROGRESS) 취미를 기준으로 데이터를 조회합니다. <br>" +
                    "- **hobbyId가 있을 경우**: 해당 ID의 취미를 기준으로 데이터를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetHomeHobbyInfoResDto.class))
            )
    })
    GetHomeHobbyInfoResDto getHomeHobbyInfo(@RequestParam(value = "hobbyId", required = false) Long hobbyId, @AuthenticationPrincipal CustomUserDetails user);

    @Operation(
            summary = "내 취미 설정 조회",
            description = "현재 진행 중인(IN_PROGRESS) 모든 취미의 설정 값들을 조회합니다. <br>" +
                    "취미 관리 페이지에서 각 취미의 시간, 목표 일수 등을 확인할 때 사용합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MyHobbySettingResDto.class))
            )
    })
    MyHobbySettingResDto myHobbySetting(@Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails user, HobbyStatus hobbyStatus);


    @Operation(
            summary = "취미 상태 변경",
            description = """
                    취미의 상태를 변경합니다.<br><br>
                    
                    <b>변경 가능 상태</b><br>
                    - IN_PROGRESS : 진행 중<br>
                    - ARCHIVED : 보관됨<br><br>
                    
                    ⚠️ 이미 동일한 상태인 경우에도 성공(200) 응답을 반환합니다.<br>
                    ⚠️ 진행 중 상태로 변경 시, 이미 진행 중인 취미가 2개라면 실패합니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "취미 상태 변경 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResDto.class),
                            examples = {
                                    @ExampleObject(
                                            name = "STATUS_CHANGED",
                                            summary = "취미 상태 변경 성공",
                                            value = """
                                                    {
                                                      "status": 200,
                                                      "success": true,
                                                      "data": {
                                                        "message": "취미 상태가 성공적으로 수정되었습니다."
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "ALREADY_SAME_STATUS",
                                            summary = "이미 동일한 상태",
                                            value = """
                                                    {
                                                      "status": 200,
                                                      "success": true,
                                                      "data": {
                                                        "message": "이미 해당 상태입니다."
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "MAX_IN_PROGRESS_HOBBY_EXCEEDED",
                                            summary = "진행 중 취미 개수 초과",
                                            value = """
                                                    {
                                                      "status": 400,
                                                      "success": false,
                                                      "data": {
                                                        "errorClassName": "MAX_IN_PROGRESS_HOBBY_EXCEEDED",
                                                        "message": "이미 진행 중인 취미는 최대 2개까지 등록할 수 있습니다."
                                                      }
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "INVALID_HOBBY_STATUS",
                                            summary = "유효하지 않은 취미 상태",
                                            value = """
                                                    {
                                                      "status": 400,
                                                      "success": false,
                                                      "data": {
                                                        "errorClassName": "INVALID_HOBBY_STATUS",
                                                        "message": "현재 취미 상태에서는 해당 작업을 수행할 수 없습니다."
                                                      }
                                                    }
                                                    """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "취미 소유자가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 403,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "NOT_HOBBY_OWNER",
                                                "message": "취미 소유자가 아닙니다."
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
    @PatchMapping("/{hobbyId}/status")
    MessageResDto updateHobbyStatus(
            @Parameter(description = "취미 ID", example = "1")
            @PathVariable(value = "hobbyId") Long hobbyId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "변경할 취미 상태",
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "IN_PROGRESS",
                                            summary = "취미 진행 상태로 변경",
                                            value = """
                                                    {
                                                      "hobbyStatus": "IN_PROGRESS"
                                                    }
                                                    """
                                    ),
                                    @ExampleObject(
                                            name = "ARCHIVED",
                                            summary = "취미 보관 상태로 변경",
                                            value = """
                                                    {
                                                      "hobbyStatus": "ARCHIVED"
                                                    }
                                                    """
                                    )
                            }
                    )
            )
            @RequestBody @Valid UpdateHobbyStatusReqDto reqDto,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "취미 활동 목록 조회",
            description = """
                    특정 취미에 등록된 활동(Activity) 목록을 조회합니다.<br><br>
                    
                    <b>비즈니스 규칙</b><br>
                    - 스티커가 있는 활동: 수정 가능 / 삭제 불가<br>
                    - 스티커가 없는 활동: 수정 가능 / 삭제 가능<br><br>
                    
                    ⚠️ 취미 소유자만 조회할 수 있습니다.
                    """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "활동 목록 조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GetActivityListResDto.class),
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 200,
                                              "success": true,
                                              "data": {
                                                "activities": [
                                                  {
                                                    "activityId": 1,
                                                    "content": "미라클 모닝 아침 독서",
                                                    "aiRecommended": false,
                                                    "deletable": false,
                                                    "stickers": [
                                                      {
                                                        "activityRecordId": 1,
                                                        "sticker": "smile.jpg"
                                                      },
                                                      {
                                                        "activityRecordId": 2,
                                                        "sticker": "smile.jpg"
                                                      }
                                                    ]
                                                  },
                                                  {
                                                    "activityId": 2,
                                                    "content": "미라클 모닝 아침 독서",
                                                    "aiRecommended": false,
                                                    "deletable": true,
                                                    "stickers": []
                                                  },
                                                  {
                                                    "activityId": 3,
                                                    "content": "미라클 모닝 아침 독서",
                                                    "aiRecommended": true,
                                                    "deletable": false,
                                                    "stickers": [
                                                      {
                                                        "activityRecordId": 3,
                                                        "sticker": "smile.jpg"
                                                      }
                                                    ]
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "취미 소유자가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            {
                                              "status": 403,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "NOT_HOBBY_OWNER",
                                                "message": "취미 소유자가 아닙니다."
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
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "현재 취미 상태에서는 작업을 수행할 수 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "INVALID_HOBBY_STATUS",
                                    summary = "유효하지 않은 취미 상태",
                                    value = """
                                            {
                                              "status": 400,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "INVALID_HOBBY_STATUS",
                                                "message": "현재 취미 상태에서는 해당 작업을 수행할 수 없습니다."
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    GetActivityListResDto getActivityList(
            @Parameter(description = "취미 ID", example = "1")
            @PathVariable(value = "hobbyId") Long hobbyId,

            @AuthenticationPrincipal CustomUserDetails user
    );


    @Operation(
            summary = "취미 시간 수정",
            description = "취미에 설정된 1회 수행 시간을 수정합니다. " +
                    "본인 소유의 취미이며, 수정 가능한 상태(IN_PROGRESS 등)인 경우에만 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "취미 시간 수정 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 / 취미 상태 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "INVALID_HOBBY_STATUS",
                                    summary = "유효하지 않은 취미 상태",
                                    value = """
                                            {
                                              "status": 400,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "INVALID_HOBBY_STATUS",
                                                "message": "현재 취미 상태에서는 해당 작업을 수행할 수 없습니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "취미 소유자 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "NOT_HOBBY_OWNER",
                                    summary = "취미 소유자 아님",
                                    value = """
                                            {
                                              "status": 403,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "NOT_HOBBY_OWNER",
                                                "message": "해당 취미에 대한 권한이 없습니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "취미 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "HOBBY_NOT_FOUND",
                                    summary = "취미를 찾을 수 없음",
                                    value = """
                                            {
                                              "status": 404,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "HOBBY_NOT_FOUND",
                                                "message": "해당 취미를 찾을 수 없습니다."
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    MessageResDto updateHobbyTime(
            @PathVariable Long hobbyId,
            @RequestBody @Valid HobbyTimePayload reqDto,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "취미 수행 횟수 수정",
            description = "취미의 누적 수행 횟수를 수정합니다. " +
                    "본인 소유이며 수정 가능한 상태인 취미만 수정할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수행 횟수 수정 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "취미 상태 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "INVALID_HOBBY_STATUS",
                                    summary = "유효하지 않은 취미 상태",
                                    value = """
                                            {
                                              "status": 400,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "INVALID_HOBBY_STATUS",
                                                "message": "현재 취미 상태에서는 해당 작업을 수행할 수 없습니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "취미 소유자 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "NOT_HOBBY_OWNER",
                                    summary = "취미 소유자 아님",
                                    value = """
                                            {
                                              "status": 403,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "NOT_HOBBY_OWNER",
                                                "message": "해당 취미에 대한 권한이 없습니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "취미 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "HOBBY_NOT_FOUND",
                                    summary = "취미를 찾을 수 없음",
                                    value = """
                                            {
                                              "status": 404,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "HOBBY_NOT_FOUND",
                                                "message": "해당 취미를 찾을 수 없습니다."
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    public MessageResDto updateExecutionCount(
            @PathVariable Long hobbyId,
            @RequestBody @Valid ExecutionCountPayload reqDto,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "취미 목표 일수 수정",
            description = "취미의 목표 수행 일수를 수정합니다. " +
                    "본인 소유이며 수정 가능한 상태의 취미만 변경할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "목표 일수 수정 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "취미 상태 오류",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "INVALID_HOBBY_STATUS",
                                    summary = "유효하지 않은 취미 상태",
                                    value = """
                                            {
                                              "status": 400,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "INVALID_HOBBY_STATUS",
                                                "message": "현재 취미 상태에서는 해당 작업을 수행할 수 없습니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "취미 소유자 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "NOT_HOBBY_OWNER",
                                    summary = "취미 소유자 아님",
                                    value = """
                                            {
                                              "status": 403,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "NOT_HOBBY_OWNER",
                                                "message": "해당 취미에 대한 권한이 없습니다."
                                              }
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "취미 없음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "HOBBY_NOT_FOUND",
                                    summary = "취미를 찾을 수 없음",
                                    value = """
                                            {
                                              "status": 404,
                                              "success": false,
                                              "data": {
                                                "errorClassName": "HOBBY_NOT_FOUND",
                                                "message": "해당 취미를 찾을 수 없습니다."
                                              }
                                            }
                                            """
                            )
                    )
            )
    })
    public MessageResDto updateGoalDays(
            @PathVariable Long hobbyId,
            @RequestBody @Valid GoalDaysPayload reqDto,
            @AuthenticationPrincipal CustomUserDetails user
    );


    @Operation(
            summary = "취미 기간 연장 또는 아카이브",
            description = """
        취미 기간을 연장하거나 아카이브 처리합니다.

        - CONTINUE: 스티커 66개 이상인 경우에만 연장 가능
        - ARCHIVE: 취미를 아카이브 처리
        """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "취미 기간 설정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = SetHobbyExtensionResDto.class),
                            examples = @ExampleObject(
                                    value = """
                {
                  "status": 200,
                  "success": true,
                  "data": {
                    "hobbyId": 18,
                    "type": "CONTINUE",
                    "message": "취미 기간 설정이 정상적으로 처리되었습니다."
                  }
                }
                """
                            )
                    )
            ),

            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "기간 설정 없는 취미",
                                            value = """
                    {
                      "status": 400,
                      "success": false,
                      "data": {
                        "errorClassName": "HOBBY_PERIOD_NOT_SET",
                        "message": "기간 설정이 없는 취미입니다."
                      }
                    }
                    """
                                    ),
                                    @ExampleObject(
                                            name = "스티커 부족",
                                            value = """
                    {
                      "status": 400,
                      "success": false,
                      "data": {
                        "errorClassName": "HOBBY_STICKER_NOT_ENOUGH",
                        "message": "스티커가 66개 이상 채워지지 않았습니다."
                      }
                    }
                    """
                                    ),
                                    @ExampleObject(
                                            name = "잘못된 타입",
                                            value = """
                    {
                      "status": 400,
                      "success": false,
                      "data": {
                        "errorClassName": "INVALID_HOBBY_EXTENSION_TYPE",
                        "message": "유효하지 않은 취미 기간 설정 타입입니다."
                      }
                    }
                    """
                                    )
                            }
                    )
            ),

            @ApiResponse(
                    responseCode = "403",
                    description = "취미 소유자가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                {
                  "status": 403,
                  "success": false,
                  "data": {
                    "errorClassName": "NOT_HOBBY_OWNER",
                    "message": "취미 소유자가 아닙니다."
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
    SetHobbyExtensionResDto setHobbyExtension(Long hobbyId, SetHobbyExtensionReqDto reqDto, CustomUserDetails user);
}
