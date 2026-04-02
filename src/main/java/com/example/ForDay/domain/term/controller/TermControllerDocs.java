package com.example.ForDay.domain.term.controller;

import com.example.ForDay.domain.term.dto.request.RegisterTermsConsentReqDto;
import com.example.ForDay.domain.term.dto.response.RegisterTermsConsentResDto;
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
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Terms", description = "약관 관련 API") // 태그명을 Terms로 수정 (기존 Activity에서 변경)
public interface TermControllerDocs {

    @Operation(
            summary = "약관 동의 내역 등록",
            description = "사용자의 서비스 이용 약관 동의 내역을 수집합니다. 소셜/게스트 로그인 후 최초 1회 수행이 필요합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "약관 동의 성공",
                    content = @Content(schema = @Schema(implementation = RegisterTermsConsentResDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (필수 약관 미동의 등)",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 400, \"success\": false, \"message\": \"잘못된 요청입니다.\"}"))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "이미 동의 내역이 존재하는 경우",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = """
                                {
                                  "status": 409,
                                  "success": false,
                                  "data": {
                                    "errorClassName": "TERMS_CONSENT_ALREADY_EXISTS",
                                    "message": "이미 동의 내역이 존재합니다."
                                  }
                                }
                                """)
                    )
            )
    })
    RegisterTermsConsentResDto registerTermsConsent(
            @RequestBody @Valid RegisterTermsConsentReqDto reqDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails user
    );
}