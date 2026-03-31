package com.example.ForDay.domain.app.controller;

import com.example.ForDay.domain.app.dto.request.UpdateFcmTokenReqDto;
import com.example.ForDay.domain.app.dto.response.UpdateFcmTokenResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "App", description = "앱 설정 및 메타데이터 관련 API")
public interface AppControllerDocs {

    @Operation(
            summary = "FCM 토큰 갱신",
            description = "특정 기기(deviceId)에 해당하는 FCM 토큰을 최신 정보로 업데이트합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "FCM 토큰 갱신 성공",
                    content = @Content(schema = @Schema(implementation = UpdateFcmTokenResDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 파라미터",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 400, \"success\": false, \"message\": \"잘못된 요청입니다.\"}"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "해당 기기에 등록된 토큰이 없는 경우",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "FCM_TOKEN_NOT_FOUND",
                                    value = """
                                        {
                                          "status": 404,
                                          "success": false,
                                          "data": {
                                            "errorClassName": "FCM_TOKEN_NOT_FOUND",
                                            "message": "등록된 FCM 토큰이 없습니다."
                                          }
                                        }
                                        """
                            )
                    )
            )
    })
    UpdateFcmTokenResDto updateFcmToken(
            @RequestBody UpdateFcmTokenReqDto reqDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails user
    );
}