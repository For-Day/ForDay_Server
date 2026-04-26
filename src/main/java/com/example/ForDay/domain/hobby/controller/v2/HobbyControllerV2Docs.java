package com.example.ForDay.domain.hobby.controller.v2;

import com.example.ForDay.domain.hobby.dto.request.HobbyCreateReqDtoV2;
import com.example.ForDay.domain.hobby.dto.request.UpdateMyHobbySettingReqDtoV2;
import com.example.ForDay.domain.hobby.dto.response.GetHomeHobbyInfoResDto;
import com.example.ForDay.domain.hobby.dto.response.HobbyCreateResDtoV2;
import com.example.ForDay.domain.hobby.dto.response.MyHobbySettingResDtoV2;
import com.example.ForDay.domain.hobby.dto.response.UpdateMyHobbySettingResDtoV2;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Hobby V2", description = "취미 관련 V2 API")
public interface HobbyControllerV2Docs {

    @Operation(
            summary = "취미 일괄 등록 (V2)",
            description = "사용자의 취미를 리스트 형태로 일괄 등록합니다. 첫 등록 시 온보딩 상태가 완료로 변경됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "취미 등록 성공"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "검증 실패 또는 최대 개수 초과",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "MAX_HOBBY_EXCEEDED",
                                    value = "{\"status\": 400, \"message\": \"취미는 최대 10개까지 등록할 수 있습니다.\"}"
                            ),
                            @ExampleObject(
                                    name = "VALIDATION_ERROR",
                                    value = "{\"status\": 400, \"message\": \"취미 이름은 20자 이내여야 합니다.\"}"
                            )
                    })
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 존재하는 취미",
                    content = @Content(examples = @ExampleObject(
                            name = "ALREADY_HAVE_HOBBY",
                            value = "{\"status\": 409, \"message\": \"이미 가지고 있는 취미입니다.\"}"
                    ))
            )
    })
    HobbyCreateResDtoV2 hobbyCreate(
            @RequestBody HobbyCreateReqDtoV2 reqDto,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "취미 설정 화면 조회 (V2)",
            description = "사용자의 취미 설정 목록을 조회합니다. <br>" +
                    "1. **진행 중인 취미**: 사용자가 지정한 순서(sequence) 오름차순으로 정렬됩니다. <br>" +
                    "2. **숨긴 취미**: 최신 등록순(createdAt) 내림차순으로 정렬됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MyHobbySettingResDtoV2.class))
            )
    })
    MyHobbySettingResDtoV2 myHobbySetting(@AuthenticationPrincipal CustomUserDetails user);

    @Operation(
            summary = "취미 설정 업데이트 (V2)",
            description = "사용자의 취미 상태(진행 중/숨김)와 정렬 순서를 일괄 변경합니다. <br>" +
                    "변경 완료 후 최신화된 전체 취미 리스트(진행 중/숨김)를 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "업데이트 성공",
                    content = @Content(schema = @Schema(implementation = UpdateMyHobbySettingResDtoV2.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (검증 실패)",
                    content = @Content(examples = @ExampleObject(
                            name = "VALIDATION_ERROR",
                            value = "{\"status\": 400, \"message\": \"hobbyId는 필수 값입니다.\"}"
                    ))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "취미 정보 없음 (요청값 중 존재하지 않는 취미 id가 있을 때)",
                    content = @Content(examples = @ExampleObject(
                            name = "HOBBY_NOT_FOUND",
                            value = "{\"status\": 404, \"message\": \"해당 취미를 찾을 수 없습니다.\"}"
                    ))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "순서 중복 오류 (요청된 progressHobbyList 내에 동일한 sequence 값이 존재할 때)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "DUPLICATE_SEQUENCE",
                                    value = """
                                {
                                  "status": 409,
                                  "success": false,
                                  "data": {
                                    "errorClassName": "DUPLICATE_SEQUENCE",
                                    "message": "요청 내에 중복된 순서가 존재합니다."
                                  }
                                }
                                """
                            )
                    )
            )
    })
    UpdateMyHobbySettingResDtoV2 updateMyHobbySetting(
            @RequestBody @Valid UpdateMyHobbySettingReqDtoV2 reqDto,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "홈 대시보드 정보 조회",
            description = "특정 취미의 상세 정보와 AI 인사이트를 조회합니다. hobbyId가 없으면 순서(sequence)가 1번인 취미를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (취미가 없는 경우 기본 닉네임과 비어있는 데이터 반환)",
                    content = @Content(schema = @Schema(implementation = GetHomeHobbyInfoResDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "취미 조회 실패 (존재하지 않는 hobbyId 요청 시)",
                    content = @Content(examples = @ExampleObject(
                            name = "HOBBY_NOT_FOUND",
                            value = """
                {
                  "status": 404,
                  "success": false,
                  "message": "해당 취미를 찾을 수 없습니다."
                }
                """
                    ))
            )
    })
    GetHomeHobbyInfoResDto getHomeHobbyInfo(
            @Parameter(description = "조회할 취미 ID (미입력 시 sequence 1번 자동 조회)", example = "38")
            @RequestParam(value = "hobbyId", required = false) Long hobbyId,
            @AuthenticationPrincipal CustomUserDetails user
    );
}