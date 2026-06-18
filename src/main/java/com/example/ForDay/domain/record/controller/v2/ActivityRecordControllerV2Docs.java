package com.example.ForDay.domain.record.controller.v2;

import com.example.ForDay.domain.record.dto.request.ActivityRecordReqDtoV2;
import com.example.ForDay.domain.record.dto.request.RecordSearchConditionReqDto;
import com.example.ForDay.domain.record.dto.request.UpdateActivityRecordReqDtoV2;
import com.example.ForDay.domain.record.dto.response.ActivityRecordResDto;
import com.example.ForDay.domain.record.dto.response.GetRecordDetailResDtoV2;
import com.example.ForDay.domain.record.dto.response.ReactionSummaryResDto;
import com.example.ForDay.domain.record.dto.response.ReactionTabScrollResDto;
import com.example.ForDay.domain.record.dto.response.UpdateActivityRecordResDtoV2;
import com.example.ForDay.domain.record.type.RecordReactionType;
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

import java.util.List;

@Tag(name = "activityRecordV2", description = "활동 기록 관련 API V2")
public interface ActivityRecordControllerV2Docs {

    @Operation(
            summary = "활동 기록 상세 조회 V2",
            description = "활동 기록의 상세 정보와 이전/다음 기록 ID를 조회합니다. context 값에 따라 요구되는 파라미터가 달라집니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(examples = {
                    @ExampleObject(name = "HOBBY_ID_REQUIRED", value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"HOBBY_ID_REQUIRED\", \"message\": \"특정 취미 소식 조회 시 취미 ID는 필수입니다.\"}}"),
                    @ExampleObject(name = "ACCESS_DENIED_FOR_GUEST", value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"ACCESS_DENIED_FOR_GUEST\", \"message\": \"게스트는 소식에 대한 접근 권한이 존재하지 않습니다.\"}}")
            })),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content(examples = {
                    @ExampleObject(name = "FRIEND_ONLY_ACCESS", value = "{\"status\": 403, \"success\": false, \"data\": {\"errorClassName\": \"FRIEND_ONLY_ACCESS\", \"message\": \"이 글은 친구만 조회할 수 있습니다.\"}}"),
                    @ExampleObject(name = "PRIVATE_RECORD", value = "{\"status\": 403, \"success\": false, \"data\": {\"errorClassName\": \"PRIVATE_RECORD\", \"message\": \"이 글은 작성자만 볼 수 있습니다.\"}}")
            })),
            @ApiResponse(responseCode = "404", description = "기록 없음", content = @Content(examples = {
                    @ExampleObject(name = "ACTIVITY_RECORD_NOT_FOUND", value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"ACTIVITY_RECORD_NOT_FOUND\", \"message\": \"존재하지 않는 활동 기록입니다.\"}}")
            }))
    })
    @GetMapping("/{recordId}")
    GetRecordDetailResDtoV2 getRecordDetailV2(
            @Parameter(description = "활동 기록 ID", example = "129") @PathVariable(name = "recordId") Long recordId,
            @Valid @ModelAttribute RecordSearchConditionReqDto condition,
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "취미 필터링 ID 리스트 (STORY_HOBBY, USER_FEED 시 필요)", example = "1,2,3") @RequestParam(name = "hobbyIds", required = false) List<Long> hobbyIds,
            @Parameter(description = "알림 목록 또는 푸시 알림 내역을 통해서 기록 조회시 해당 알림을 읽음 처리 하기 위한 파라미터입니다.", example = "12") @RequestParam(name = "notificationId", required = false) Long notificationId);

    @Operation(summary = "감정별 반응 요약 및 초기 목록 조회", description = "기록에 대한 반응 수 요약과 각 탭(ALL, AWESOME 등)별 초기 사용자 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ReactionSummaryResDto.class)))
    })
    @GetMapping("/{recordId}/reactions/summary")
    ReactionSummaryResDto getReactionSummary(
            @Parameter(description = "활동 기록 ID", example = "141") @PathVariable Long recordId,
            @Parameter(description = "각 탭별 초기 조회 갯수", example = "20") @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(summary = "반응 탭별 무한 스크롤 추가 조회", description = "특정 반응 타입에 대해 무한 스크롤로 사용자 목록을 추가 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "추가 조회 성공", content = @Content(schema = @Schema(implementation = ReactionTabScrollResDto.class)))
    })
    @GetMapping("/{recordId}/reactions/tab")
    ReactionTabScrollResDto getReactionTabScroll(
            @Parameter(description = "활동 기록 ID", example = "141") @PathVariable Long recordId,
            @Parameter(description = "조회할 감정 유형 (미입력 시 전체)", example = "AWESOME") @RequestParam(required = false) RecordReactionType type,
            @Parameter(description = "이전 조회에서 마지막으로 조회된 reactionId", example = "36") @RequestParam(required = false) Long lastReactionId,
            @Parameter(description = "추가 조회할 갯수", example = "10") @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "활동 기록하기 V2",
            description = """
                    활동을 기록합니다. activityId와 activityContent는 둘 중 하나만 입력해야 합니다.
                    - activityId가 있는 경우: 기존 활동을 사용하여 기록합니다. (activityContent는 null이어야 합니다.)
                    - activityContent가 있는 경우: 새 활동을 생성한 뒤 기록합니다. (activityId는 null이어야 합니다.)
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "기록 성공",
                    content = @Content(schema = @Schema(implementation = ActivityRecordResDto.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(examples = {
                    @ExampleObject(name = "INVALID_INPUT_VALUE", value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"INVALID_INPUT_VALUE\", \"message\": \"activityId가 null이면 activityContent는 필수입니다. activityId가 있으면 activityContent는 null이어야 합니다.\"}}"),
                    @ExampleObject(name = "INVALID_HOBBY_STATUS", value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"INVALID_HOBBY_STATUS\", \"message\": \"현재 취미 상태에서는 해당 작업을 수행할 수 없습니다.\"}}"),
                    @ExampleObject(name = "STICKER_COMPLETION_REACHED", value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"STICKER_COMPLETION_REACHED\", \"message\": \"해당 취미의 스티커 수가 이미 최대치에 도달했습니다.\"}}")
            })),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음", content = @Content(examples = {
                    @ExampleObject(name = "HOBBY_NOT_FOUND", value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"HOBBY_NOT_FOUND\", \"message\": \"존재하지 않는 취미입니다.\"}}"),
                    @ExampleObject(name = "ACTIVITY_NOT_FOUND", value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"ACTIVITY_NOT_FOUND\", \"message\": \"존재하지 않는 활동입니다.\"}}")
            }))
    })
    @PostMapping
    ActivityRecordResDto recordActivity(
            @Valid @RequestBody ActivityRecordReqDtoV2 reqDto,
            @AuthenticationPrincipal CustomUserDetails user
    );

    @Operation(
            summary = "활동 기록 수정 V2",
            description = """
                    활동 기록을 수정합니다.
                    - activityId: 연결된 활동을 변경할 경우 입력합니다. (생략 시 기존 활동 유지)
                    - images: 기존 이미지를 모두 대체합니다. 빈 리스트 전달 시 이미지가 전체 삭제됩니다.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = UpdateActivityRecordResDtoV2.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(examples = {
                    @ExampleObject(name = "INVALID_INPUT_VALUE", value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"INVALID_INPUT_VALUE\", \"message\": \"스티커는 필수입니다.\"}}")
            })),
            @ApiResponse(responseCode = "403", description = "수정 권한 없음", content = @Content(examples = {
                    @ExampleObject(name = "ACCESS_DENIED", value = "{\"status\": 403, \"success\": false, \"data\": {\"errorClassName\": \"ACCESS_DENIED\", \"message\": \"본인의 기록만 수정할 수 있습니다.\"}}")
            })),
            @ApiResponse(responseCode = "404", description = "리소스를 찾을 수 없음", content = @Content(examples = {
                    @ExampleObject(name = "ACTIVITY_RECORD_NOT_FOUND", value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"ACTIVITY_RECORD_NOT_FOUND\", \"message\": \"존재하지 않는 활동 기록입니다.\"}}"),
                    @ExampleObject(name = "ACTIVITY_NOT_FOUND", value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"ACTIVITY_NOT_FOUND\", \"message\": \"존재하지 않는 활동입니다.\"}}")
            }))
    })
    @PutMapping("/{recordId}")
    UpdateActivityRecordResDtoV2 updateActivityRecord(
            @Parameter(description = "활동 기록 ID", example = "42") @PathVariable(name = "recordId") Long recordId,
            @Valid @RequestBody UpdateActivityRecordReqDtoV2 reqDto,
            @AuthenticationPrincipal CustomUserDetails user
    );
}