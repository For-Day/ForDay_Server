package com.example.ForDay.domain.recent.controller;

import com.example.ForDay.domain.recent.dto.response.GetRecentKeywordResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "Recent Keyword", description = "최근 검색어 관련 API")
public interface RecentControllerDocs {

    @Operation(
            summary = "최근 검색어 목록 조회",
            description = "사용자의 최근 검색어 목록을 최대 5개까지 최신순으로 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = GetRecentKeywordResDto.class))
            )
    })
    GetRecentKeywordResDto getRecentKeyword(@AuthenticationPrincipal CustomUserDetails user);

}
