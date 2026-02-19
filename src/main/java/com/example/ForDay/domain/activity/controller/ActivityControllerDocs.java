package com.example.ForDay.domain.activity.controller;

import com.example.ForDay.domain.activity.dto.request.UpdateActivityReqDto;
import com.example.ForDay.domain.activity.dto.response.GetAiRecommendItemsResDto;
import com.example.ForDay.domain.activity.type.AIItemType;
import com.example.ForDay.domain.hobby.dto.request.*;
import com.example.ForDay.domain.hobby.dto.response.*;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.global.common.error.ErrorResponse;
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

@Tag(name = "Activity", description = "활동 관련 API")
public interface ActivityControllerDocs {

    @Operation(
            summary = "활동 내용 수정",
            description = """
                특정 활동(Activity)의 내용을 수정합니다.

                ⚠️ 주의사항
                - 진행 중(IN_PROGRESS)인 취미의 활동만 수정 가능합니다.
                - 활동 소유자만 수정할 수 있습니다.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "활동 수정 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "취미 상태가 진행 중이 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "INVALID_HOBBY_STATUS",
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
                    description = "활동 소유자가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
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
                    description = "활동이 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
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
            )
    })
    MessageResDto updateActivity(
            @PathVariable(name = "activityId")
            @Parameter(description = "수정할 활동 ID", example = "1")
            Long activityId,

            @RequestBody
            @Valid
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "활동 수정 요청 DTO",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = UpdateActivityReqDto.class)
                    )
            )
            UpdateActivityReqDto reqDto,

            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "활동 삭제",
            description = """
                특정 활동(Activity)을 삭제합니다.

                ❗ 삭제 조건
                - 활동 소유자만 삭제할 수 있습니다.
                - 스티커가 없는 활동만 삭제 가능합니다.
                - 진행 중(IN_PROGRESS)인 취미의 활동만 삭제 가능합니다.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "활동 삭제 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MessageResDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "삭제할 수 없는 상태",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "INVALID_HOBBY_STATUS",
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
                                    ),
                                    @ExampleObject(
                                            name = "ACTIVITY_NOT_DELETABLE",
                                            value = """
                                        {
                                          "status": 400,
                                          "success": false,
                                          "data": {
                                            "errorClassName": "ACTIVITY_NOT_DELETABLE",
                                            "message": "해당 활동은 삭제할 수 없습니다."
                                          }
                                        }
                                        """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "활동 소유자가 아님",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
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
                    description = "활동이 존재하지 않음",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
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
            )
    })
    MessageResDto deleteActivity(
            @PathVariable
            @Parameter(description = "삭제할 활동 ID", example = "1")
            Long activityId,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "AI 추천 활동 아이템 조회",
            description = "특정 취미(hobbyId)에 대해 이전에 추천받았던 활동 아이템 리스트를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = GetAiRecommendItemsResDto.class)
                    )
            )
    })
    GetAiRecommendItemsResDto getAiRecommendItems(
            @Parameter(
                    description = "조회하고자 하는 활동 추천 리스트의 취미 ID",
                    required = true,
                    example = "6356892"
            )
            @RequestParam(name = "hobbyId") Long hobbyId,
            @RequestParam(name = "type") AIItemType type,
            @AuthenticationPrincipal CustomUserDetails user
    );


}
