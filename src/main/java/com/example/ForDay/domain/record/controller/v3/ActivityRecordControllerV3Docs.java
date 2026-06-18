package com.example.ForDay.domain.record.controller.v3;

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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "activityRecordV3", description = "활동 기록 관련 API V3")
public interface ActivityRecordControllerV3Docs {

    @Operation(
            summary = "기록 조회 V3",
            description = "recordId로 활동 기록 상세 정보를 조회합니다. 단일 imageUrl 대신 이미지 목록을 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetRecordDetailResDtoV3.class))),
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
            @AuthenticationPrincipal CustomUserDetails user
    );
}
