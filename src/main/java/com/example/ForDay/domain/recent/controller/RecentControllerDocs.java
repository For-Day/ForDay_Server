package com.example.ForDay.domain.recent.controller;

import com.example.ForDay.domain.recent.dto.response.DeleteAllRecentKeywordResDto;
import com.example.ForDay.domain.recent.dto.response.DeleteRecentKeywordResDto;
import com.example.ForDay.domain.recent.dto.response.GetRecentKeywordResDto;
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
import org.springframework.web.bind.annotation.PathVariable;

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

    @Operation(
            summary = "최근 검색어 전체 삭제",
            description = "사용자의 최근 검색어 목록을 모두 삭제합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(schema = @Schema(implementation = DeleteAllRecentKeywordResDto.class))
            )
    })
    DeleteAllRecentKeywordResDto deleteAllRecentKeyword(@AuthenticationPrincipal CustomUserDetails user);

    @Operation(
            summary = "최근 검색어 개별 삭제",
            description = "특정 최근 검색어(recentId)를 선택하여 삭제합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "삭제 성공",
                    content = @Content(schema = @Schema(implementation = DeleteRecentKeywordResDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 검색어 ID일 때",
                    content = @Content(examples = @ExampleObject(value = """
            {
              "status": 404,
              "success": false,
              "data": {
                "errorClassName": "KEYWORD_NOT_FOUND",
                "message": "존재하지 않는 검색어입니다."
              }
            }
            """))
            )
    })
    DeleteRecentKeywordResDto deleteRecentKeyword(
            @Parameter(description = "삭제할 검색어의 ID (타임스탬프)", example = "1769846184063")
            @PathVariable(name = "recentId") Long recentId, @AuthenticationPrincipal CustomUserDetails user);
}
