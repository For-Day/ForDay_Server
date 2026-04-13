package com.example.ForDay.domain.hobby.controller.v2;

import com.example.ForDay.domain.hobby.dto.request.HobbyCreateReqDtoV2;
import com.example.ForDay.domain.hobby.dto.response.HobbyCreateResDtoV2;
import com.example.ForDay.global.oauth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

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
}