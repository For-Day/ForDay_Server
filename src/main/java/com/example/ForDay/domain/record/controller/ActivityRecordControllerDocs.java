package com.example.ForDay.domain.record.controller;

import com.example.ForDay.domain.record.dto.request.ReactToRecordReqDto;
import com.example.ForDay.domain.record.dto.request.UpdateRecordVisibilityReqDto;
import com.example.ForDay.domain.record.dto.response.*;
import com.example.ForDay.domain.record.service.ActivityRecordService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "activityRecord", description = "활동 기록 관련 API")
public interface ActivityRecordControllerDocs {

    @Operation(summary = "기록 상세 조회", description = "기록의 상세 내용과 리액션 상태를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음 (비공개 또는 친구 전용)",
                    content = @Content(examples = {
                            @ExampleObject(name = "나만 보기 기록", value = "{\"status\": 403, \"success\": false, \"data\": {\"errorClassName\": \"PRIVATE_RECORD\", \"message\": \"이 글은 작성자만 볼 수 있습니다.\"}}"),
                            @ExampleObject(name = "친구 공개 기록", value = "{\"status\": 403, \"success\": false, \"data\": {\"errorClassName\": \"FRIEND_ONLY_ACCESS\", \"message\": \"이 글은 친구만 조회할 수 있습니다.\"}}")
                    })
            ),
            @ApiResponse(responseCode = "404", description = "기록 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"ACTIVITY_RECORD_NOT_FOUND\", \"message\": \"존재하지 않는 활동 기록입니다.\"}}"))
            )
    })
    GetRecordDetailResDto getRecordDetail(@PathVariable(name = "activityRecordId") Long activityRecordId, @AuthenticationPrincipal CustomUserDetails user);

    @Operation(
            summary = "기록 공개 범위 수정",
            description = "특정 기록의 공개 범위를 수정합니다. 본인의 기록만 수정할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "공개 범위 수정 성공",
                    content = @Content(schema = @Schema(implementation = UpdateRecordVisibilityResDto.class),
                            examples = @ExampleObject(value = "{\"status\": 200, \"success\": true, \"data\": {\"message\": \"공개 범위가 정상적으로 변경되었습니다.\", \"previousVisibility\": \"PRIVATE\", \"newVisibility\": \"FRIEND\"}}"))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 403, \"success\": false, \"data\": {\"errorClassName\": \"NOT_ACTIVITY_OWNER\", \"message\": \"활동 소유자가 아닙니다.\"}}"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "기록을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"ACTIVITY_RECORD_NOT_FOUND\", \"message\": \"존재하지 않는 활동 기록입니다.\"}}"))
            )
    })
    UpdateRecordVisibilityResDto updateRecordVisibility(
            @Parameter(description = "수정할 활동 기록의 ID", example = "1")
            @PathVariable(name = "activityRecordId") Long activityRecordId,
            @RequestBody @Valid UpdateRecordVisibilityReqDto reqDto,
            @AuthenticationPrincipal CustomUserDetails user);

    @Operation(
            summary = "리액션 유저 목록 조회",
            description = "특정 리액션을 남긴 유저들 중 아직 확인하지 않은(unread) 목록을 최신순으로 조회합니다. 기록 소유자만 접근 가능합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetRecordReactionUsersResDto.class),
                            examples = @ExampleObject(value = "{\"status\": 200, \"success\": true, \"data\": {\"reactionType\": \"AWESOME\", \"reactionUsers\": [{\"userId\": \"75e5f503-667a-40e8-8f90-f592a2022a5d\", \"nickname\": \"유지\", \"profileImageUrl\": \"https://forday-s3.amazonaws.com/profiles/yuji.png\", \"reactedAt\": \"2026-01-23T17:34:53\"}, {\"userId\": \"a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6\", \"nickname\": \"행복한토끼\", \"profileImageUrl\": null, \"reactedAt\": \"2026-01-23T16:20:10\"}]}}"))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 부족",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 403, \"success\": false, \"data\": {\"errorClassName\": \"NOT_ACTIVITY_OWNER\", \"message\": \"활동 소유자가 아닙니다.\"}}"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "기록 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"ACTIVITY_RECORD_NOT_FOUND\", \"message\": \"존재하지 않는 활동 기록입니다.\"}}"))
            )
    })
    GetRecordReactionUsersResDto getRecordReactionUsers(
            @Parameter(description = "활동 기록 ID", example = "1") @PathVariable Long recordId,
            @Parameter(description = "조회할 리액션 타입", example = "AWESOME") @RequestParam RecordReactionType reactionType,
            @RequestParam(name = "lastUserId") String lastUserId,
            @RequestParam(name = "size") Integer size,
            @AuthenticationPrincipal CustomUserDetails user
    );


    @Operation(
            summary = "리액션 등록",
            description = "활동 기록에 리액션을 남깁니다. 전체 공개글이거나 친구 관계인 경우에만 가능하며, 중복 리액션은 허용되지 않습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "리액션 등록 성공",
                    content = @Content(schema = @Schema(implementation = ReactToRecordResDto.class),
                            examples = @ExampleObject(value = "{\"status\": 200, \"success\": true, \"data\": {\"message\": \"반응이 정상적으로 등록되었습니다.\", \"reactionType\": \"GREAT\", \"recordId\": 123}}"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (중복 리액션)",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"DUPLICATE_REACTION\", \"message\": \"해당 기록에는 이미 같은 반응을 하셨습니다.\"}}"))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 없음 (비공개 또는 친구 아님)",
                    content = @Content(examples = {
                            @ExampleObject(name = "친구 미등록", value = "{\"status\": 403, \"success\": false, \"data\": {\"errorClassName\": \"FRIEND_ONLY_ACCESS\", \"message\": \"이 글은 친구만 조회할 수 있습니다.\"}}"),
                            @ExampleObject(name = "나만 보기 기록", value = "{\"status\": 403, \"success\": false, \"data\": {\"errorClassName\": \"PRIVATE_RECORD\", \"message\": \"이 글은 작성자만 볼 수 있습니다.\"}}")
                    })
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "기록 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"ACTIVITY_RECORD_NOT_FOUND\", \"message\": \"존재하지 않는 활동 기록입니다.\"}}"))
            )
    })
    @PostMapping("/{recordId}/reactions")
    ReactToRecordResDto reactToRecord(
            @Parameter(description = "활동 기록 ID", example = "123") @PathVariable Long recordId,
            @Parameter(description = "리액션 종류 (AWESOME, GREAT, AMAZING, FIGHTING)", example = "GREAT")   @RequestBody ReactToRecordReqDto reqDto,
            @AuthenticationPrincipal CustomUserDetails user
    );


    @Operation(
            summary = "리액션 취소",
            description = "본인이 남긴 리액션을 취소(삭제)합니다. 기록 ID와 리액션 타입을 파라미터로 전달해야 합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "리액션 취소 성공",
                    content = @Content(schema = @Schema(implementation = CancelReactToRecordResDto.class),
                            examples = @ExampleObject(value = "{\"status\": 200, \"success\": true, \"data\": {\"message\": \"리액션이 정상적으로 취소되었습니다.\", \"reactionType\": \"GREAT\", \"recordId\": 123}}"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "찾을 수 없음 (기록 또는 리액션)",
                    content = @Content(examples = {
                            @ExampleObject(name = "기록 없음", value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"ACTIVITY_RECORD_NOT_FOUND\", \"message\": \"존재하지 않는 활동 기록입니다.\"}}"),
                            @ExampleObject(name = "리액션 없음", value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"REACTION_NOT_FOUND\", \"message\": \"해당 리액션을 찾을 수 없거나 이미 취소되었습니다.\"}}")
                    })
            )
    })
    CancelReactToRecordResDto cancelReactToRecord(
            @Parameter(description = "활동 기록 ID", example = "123") @PathVariable(name = "recordId") Long recordId,
            @Parameter(description = "취소할 리액션 타입 (AWESOME: 멋져요, GREAT: 짱이야, AMAZING: 대단해, FIGHTING: 화이팅)", example = "GREAT")
            @RequestParam(name = "reactionType") RecordReactionType reactionType,
            @AuthenticationPrincipal CustomUserDetails user
    );
}
