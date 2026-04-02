package com.example.ForDay.domain.notification.controller;

import com.example.ForDay.domain.notification.dto.request.UpdatePushNotificationToggleReqDto;
import com.example.ForDay.domain.notification.dto.response.GetNotificationListResDto;
import com.example.ForDay.domain.notification.dto.response.GetPushNotificationToggleResDto;
import com.example.ForDay.domain.notification.dto.response.UpdatePushNotificationToggleResDto;
import com.example.ForDay.domain.notification.type.NotificationFilterType;
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

@Tag(name = "Notification", description = "알림 관련 API")
public interface NotificationControllerDocs {

    @Operation(
            summary = "알림 목록 조회",
            description = "사용자의 알림 내역을 커서 기반 무한 스크롤로 조회합니다. 알림 권한 유무에 따른 안내 메시지가 포함됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "알림 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = GetNotificationListResDto.class))
            )
    })
    @GetMapping
    GetNotificationListResDto getNotificationList(
            @Parameter(description = "필터 타입 (ALL, RECORD, FRIEND, GROUP)", example = "ALL")
            @RequestParam(name = "filterType", required = false) NotificationFilterType filterType,
            @Parameter(description = "커서 기반 페이징을 위한 마지막 알림 ID", example = "150")
            @RequestParam(name = "lastNotificationId", required = false) Long lastNotificationId,
            @Parameter(description = "한 번에 조회할 알림 개수", example = "20")
            @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails user);

    @Operation(
            summary = "푸시 알림 설정 변경 (토글)",
            description = "앱 전체 알림(APP) 또는 게시글 관련 알림(RECORD) 설정을 개별적으로 토글합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "토글 변경 성공",
                    content = @Content(schema = @Schema(implementation = UpdatePushNotificationToggleResDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "해당 기기에 등록된 FCM 토큰이 없는 경우",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "status": 404,
                                  "success": false,
                                  "data": {
                                    "errorClassName": "FCM_TOKEN_NOT_FOUND",
                                    "message": "등록된 FCM 토큰이 없습니다."
                                  }
                                }
                                """)
                    )
            )
    })
    @PatchMapping("/toggle")
    UpdatePushNotificationToggleResDto updatePushNotificationToggle(
            @RequestBody @Valid UpdatePushNotificationToggleReqDto reqDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails user);

    @Operation(
            summary = "푸시 알림 설정 상태 조회",
            description = "현재 사용자의 앱 푸시 및 게시글 푸시 알림 활성화 여부를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "설정 상태 조회 성공",
                    content = @Content(schema = @Schema(implementation = GetPushNotificationToggleResDto.class))
            )
    })
    @GetMapping("/toggle")
    GetPushNotificationToggleResDto getPushNotificationToggle(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails user);
}