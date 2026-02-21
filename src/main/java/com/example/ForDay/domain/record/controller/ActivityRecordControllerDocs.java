package com.example.ForDay.domain.record.controller;

import com.example.ForDay.domain.record.dto.request.ReactToRecordReqDto;
import com.example.ForDay.domain.record.dto.request.ReportActivityRecordReqDto;
import com.example.ForDay.domain.record.dto.request.UpdateActivityRecordReqDto;
import com.example.ForDay.domain.record.dto.request.UpdateRecordVisibilityReqDto;
import com.example.ForDay.domain.record.dto.response.*;
import com.example.ForDay.domain.record.service.ActivityRecordService;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.record.type.StoryFilterType;
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

    @Operation(
            summary = "활동 기록 수정",
            description = "기존에 작성된 활동 기록을 수정합니다. 본인의 기록만 수정 가능하며, S3 이미지 존재 여부를 체크합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "활동 기록 수정 성공",
                    content = @Content(schema = @Schema(implementation = UpdateActivityRecordResDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "조회 실패 (기록 없음 / 활동 없음 / S3 이미지 없음)",
                    content = @Content(examples = {
                            @ExampleObject(name = "S3_IMAGE_NOT_FOUND", value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"S3_IMAGE_NOT_FOUND\", \"message\": \"S3에 해당 이미지가 존재하지 않습니다.\"}}"),
                            @ExampleObject(name = "ACTIVITY_RECORD_NOT_FOUND", value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"ACTIVITY_RECORD_NOT_FOUND\", \"message\": \"존재하지 않는 활동 기록입니다.\"}}"),
                            @ExampleObject(name = "ACTIVITY_NOT_FOUND", value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"ACTIVITY_NOT_FOUND\", \"message\": \"존재하지 않는 활동입니다.\"}}")
                    })
            )
    })
    UpdateActivityRecordResDto updateActivityRecord(
            @Parameter(description = "수정하려는 활동 기록의 ID", example = "1")
            @PathVariable(value = "recordId") Long recordId,

            @RequestBody @Valid UpdateActivityRecordReqDto reqDto,

            @AuthenticationPrincipal CustomUserDetails user);

    @Operation(
            summary = "활동 기록 삭제",
            description = "지정한 활동 기록을 삭제합니다. 본인이 작성한 기록만 삭제할 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "활동 기록 삭제 성공",
                    content = @Content(schema = @Schema(implementation = DeleteActivityRecordResDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "삭제 실패 - 기록을 찾을 수 없음",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "ACTIVITY_RECORD_NOT_FOUND",
                                    value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"ACTIVITY_RECORD_NOT_FOUND\", \"message\": \"존재하지 않는 활동 기록입니다.\"}}"
                            )
                    })
            )
    })
    DeleteActivityRecordResDto deleteActivityRecord(
            @Parameter(description = "삭제하고자 하는 활동 기록의 ID", example = "2")
            @PathVariable(value = "recordId") Long recordId,

            @AuthenticationPrincipal CustomUserDetails user);

    @Operation(
            summary = "활동 기록 스크랩",
            description = "특정 활동 기록(recordId)을 본인의 보관함에 스크랩합니다. 접근 권한(공개 범위)에 따라 제한될 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "스크랩 성공",
                    content = @Content(schema = @Schema(implementation = AddActivityRecordScrapResDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "이미 스크랩한 경우",
                    content = @Content(examples = @ExampleObject(
                            name = "DUPLICATE_SCRAP",
                            summary = "중복 스크랩 방지",
                            value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"DUPLICATE_SCRAP\", \"message\": \"해당 기록에는 이미 스크랩을 하셨습니다.\"}}"
                    ))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 부족 (비공개 또는 친구 공개)",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "FRIEND_ONLY_ACCESS",
                                    summary = "친구만 스크랩 가능",
                                    value = "{\"status\": 403, \"success\": false, \"data\": {\"errorClassName\": \"FRIEND_ONLY_ACCESS\", \"message\": \"이 글은 친구에게만 접근 권한이 있습니다.\"}}"
                            ),
                            @ExampleObject(
                                    name = "PRIVATE_RECORD",
                                    summary = "작성자만 스크랩 가능",
                                    value = "{\"status\": 403, \"success\": false, \"data\": {\"errorClassName\": \"PRIVATE_RECORD\", \"message\": \"이 글은 작성자에게만 권한이 있습니다.\"}}"
                            )
                    })
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "기록을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(
                            name = "ACTIVITY_RECORD_NOT_FOUND",
                            summary = "존재하지 않는 활동 기록",
                            value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"ACTIVITY_RECORD_NOT_FOUND\", \"message\": \"존재하지 않는 활동 기록입니다.\"}}"
                    ))
            )
    })
    AddActivityRecordScrapResDto addActivityRecordScrap(
            @Parameter(description = "스크랩하고자 하는 활동 기록의 ID", example = "1")
            @PathVariable(value = "recordId") Long recordId,
            @AuthenticationPrincipal CustomUserDetails user);

    @Operation(
            summary = "활동 기록 스크랩 취소",
            description = "기존에 스크랩했던 활동 기록을 취소합니다. 이미 취소되었거나 존재하지 않는 스크랩에 대해서도 성공 응답을 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "스크랩 취소 성공 (두 가지 케이스)",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "SUCCESS_DELETE",
                                            summary = "정상 취소 완료",
                                            value = "{\"status\": 200, \"success\": true, \"data\": {\"message\": \"스크랩 취소가 완료되었습니다.\", \"recordId\": 7, \"scraped\": false}}"
                                    ),
                                    @ExampleObject(
                                            name = "ALREADY_DELETED",
                                            summary = "이미 삭제된 상태",
                                            value = "{\"status\": 200, \"success\": true, \"data\": {\"message\": \"스크랩이 존재하지 않거나 이미 삭제되었습니다.\", \"recordId\": 7, \"scraped\": false}}"
                                    )
                            }
                    )
            )
    })
    DeleteActivityRecordScrapResDto deleteActivityRecordScrap(
            @Parameter(description = "스크랩을 취소하고자 하는 활동 기록의 ID", example = "7")
            @PathVariable(value = "recordId") Long recordId,
            @AuthenticationPrincipal CustomUserDetails user);

    @Operation(
            summary = "활동 기록 신고",
            description = "특정 활동 기록(recordId)을 신고합니다. 신고 사유를 필수로 입력해야 하며, 게시글의 공개 범위 및 상태에 따라 신고가 제한될 수 있습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "신고 완료",
                    content = @Content(schema = @Schema(implementation = ReportActivityRecordResDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (사유 누락 등)",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 400, \"success\": false, \"message\": \"신고 사유는 필수입니다.\"}"))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 부족 (비공개 또는 친구 공개)",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "FRIEND_ONLY_ACCESS",
                                    summary = "친구가 아닌 경우",
                                    value = "{\"status\": 403, \"success\": false, \"data\": {\"errorClassName\": \"FRIEND_ONLY_ACCESS\", \"message\": \"이 글은 친구에게만 접근 권한이 있습니다.\"}}"
                            ),
                            @ExampleObject(
                                    name = "PRIVATE_RECORD",
                                    summary = "나만 보기 기록인 경우",
                                    value = "{\"status\": 403, \"success\": false, \"data\": {\"errorClassName\": \"PRIVATE_RECORD\", \"message\": \"이 글은 작성자에게만 권한이 있습니다.\"}}"
                            )
                    })
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "기록을 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(
                            name = "ACTIVITY_RECORD_NOT_FOUND",
                            summary = "존재하지 않거나 접근 불가한 기록",
                            value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"ACTIVITY_RECORD_NOT_FOUND\", \"message\": \"존재하지 않는 활동 기록입니다.\"}}"
                    ))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "중복 신고",
                    content = @Content(examples = @ExampleObject(
                            name = "ALREADY_RECORD_REPORTED",
                            summary = "이미 신고한 기록",
                            value = "{\"status\": 409, \"success\": false, \"data\": {\"errorClassName\": \"ALREADY_RECORD_REPORTED\", \"message\": \"해당 기록에 이미 신고하였습니다.\"}}"
                    ))
            )
    })
    ReportActivityRecordResDto reportActivityRecord(
            @Parameter(description = "신고할 활동 기록의 ID", example = "1")
            @PathVariable(value = "recordId") Long recordId,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "신고 사유",
                    required = true,
                    content = @Content(schema = @Schema(implementation = ReportActivityRecordReqDto.class))
            )
            @RequestBody @Valid ReportActivityRecordReqDto reqDto,

            @AuthenticationPrincipal CustomUserDetails user);

    @Operation(
            summary = "소식 목록 조회 및 검색",
            description = "소식페이지에서 특정 취미의 기록들을 조회합니다. 키워드 입력 시 검색 기능이 동작하며 검색어는 자동으로 저장됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (기록이 없으면 data가 null로 반환됨)",
                    content = @Content(schema = @Schema(implementation = GetActivityRecordByStoryResDto.class))
            )
    })GetActivityRecordByStoryResDto getActivityRecordByStory(
            @Parameter(description = "취미 ID (null이면 전체 조회, 아니면 조회하고자하는 취미의 id 값)", example = "14")
            @RequestParam(name = "hobbyId", required = false) Long hobbyId,

            @Parameter(description = "마지막으로 조회된 기록 ID (null이면 처음부터 조회)", example = "42")
            @RequestParam(name = "lastRecordId", required = false) Long lastRecordId,

            @Parameter(description = "조회할 기록 개수", example = "20")
            @RequestParam(name = "size", required = false, defaultValue = "20") Integer size,

            @Parameter(description = "검색 키워드 (입력 시 최근 검색어에 저장됨)", example = "고양이")
            @RequestParam(name = "keyword", required = false) String keyword,

            @RequestParam(name = "storyFilterType", required = false, defaultValue = "ALL") StoryFilterType storyFilterType,

            @AuthenticationPrincipal CustomUserDetails user);
}
