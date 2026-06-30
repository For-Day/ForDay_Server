package com.example.ForDay.domain.record.controller.v3;

import com.example.ForDay.domain.record.dto.request.RecordSearchConditionReqDto;
import com.example.ForDay.domain.record.dto.response.GetRecordDetailResDtoV3;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "activityRecordV3", description = "활동 기록 관련 API V3")
public interface ActivityRecordControllerV3Docs {

    @Operation(
            summary = "기록 조회 V3",
            description = "recordId로 활동 기록 상세 정보를 조회합니다. 단일 imageUrl 대신 이미지 목록을 반환합니다. context 값에 따라 요구되는 파라미터가 달라집니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetRecordDetailResDtoV3.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청", content = @Content(examples = {
                    @ExampleObject(name = "HOBBY_ID_REQUIRED", value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"HOBBY_ID_REQUIRED\", \"message\": \"특정 취미 소식 조회 시 취미 ID는 필수입니다.\"}}"),
                    @ExampleObject(name = "ACCESS_DENIED_FOR_GUEST", value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"ACCESS_DENIED_FOR_GUEST\", \"message\": \"게스트는 소식에 대한 접근 권한이 존재하지 않습니다.\"}}")
            })),
            @ApiResponse(responseCode = "403", description = "접근 권한 없음", content = @Content(examples = {
                    @ExampleObject(name = "PRIVATE_RECORD", value = "{\"status\": 403, \"success\": false, \"data\": {\"errorClassName\": \"PRIVATE_RECORD\", \"message\": \"이 글은 작성자에게만 권한이 있습니다.\"}}"),
                    @ExampleObject(name = "FRIEND_ONLY_ACCESS", value = "{\"status\": 403, \"success\": false, \"data\": {\"errorClassName\": \"FRIEND_ONLY_ACCESS\", \"message\": \"이 글은 친구에게만 접근 권한이 있습니다.\"}}")
            })),
            @ApiResponse(responseCode = "404", description = "기록 없음", content = @Content(examples = {
                    @ExampleObject(name = "ACTIVITY_RECORD_NOT_FOUND", value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"ACTIVITY_RECORD_NOT_FOUND\", \"message\": \"존재하지 않는 활동 기록입니다.\"}}")
            }))
    })
    @GetMapping("/{recordId}")
    GetRecordDetailResDtoV3 getRecordDetail(
            @Parameter(description = "활동 기록 ID", example = "42") @PathVariable(name = "recordId") Long recordId,
            @Valid @ModelAttribute RecordSearchConditionReqDto condition,
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "취미 필터링 ID 리스트 (STORY_HOBBY, USER_FEED 시 필요)", example = "1,2,3") @RequestParam(name = "hobbyIds", required = false) List<Long> hobbyIds,
            @Parameter(description = "알림 목록 또는 푸시 알림 내역을 통해서 기록 조회시 해당 알림을 읽음 처리 하기 위한 파라미터입니다.", example = "12") @RequestParam(name = "notificationId", required = false) Long notificationId
    );
}
