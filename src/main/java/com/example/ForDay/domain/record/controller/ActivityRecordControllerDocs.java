package com.example.ForDay.domain.record.controller;

import com.example.ForDay.domain.record.dto.response.GetRecordDetailResDto;
import com.example.ForDay.domain.record.service.ActivityRecordService;
import com.example.ForDay.global.oauth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
