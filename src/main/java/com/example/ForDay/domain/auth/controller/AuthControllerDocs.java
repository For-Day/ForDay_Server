package com.example.ForDay.domain.auth.controller;

import com.example.ForDay.domain.auth.dto.request.AppleLoginReqDto;
import com.example.ForDay.domain.auth.dto.request.GuestLoginReqDto;
import com.example.ForDay.domain.auth.dto.request.KakaoLoginReqDto;
import com.example.ForDay.domain.auth.dto.request.RefreshReqDto;
import com.example.ForDay.domain.auth.dto.response.GuestLoginResDto;
import com.example.ForDay.domain.auth.dto.response.LoginResDto;
import com.example.ForDay.domain.auth.dto.response.RefreshResDto;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Auth", description = "인증 / 로그인 API")
public interface AuthControllerDocs {

    @Operation(
            summary = "카카오 로그인",
            description = "카카오 액세스 토큰을 이용해 로그인을 진행합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = LoginResDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 실패 (유효성 오류 또는 외부 API 오류)",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "유효성 검사 실패",
                                    summary = "필수 파라미터 누락",
                                    value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"VALIDATION_ERROR\", \"message\": \"{예: accessToken=accessToken은 필수입니다.}\"}}"
                            ),
                            @ExampleObject(
                                    name = "카카오 정보 조회 실패",
                                    summary = "카카오 API 통신 실패",
                                    value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"KAKAO_PROFILE_REQUEST_FAILED\", \"message\": \"카카오 사용자 정보 조회에 실패했습니다.\"}}"
                            )
                    })
            )
    })
    LoginResDto kakaoLogin(KakaoLoginReqDto reqDto);

    @Operation(
            summary = "애플 로그인",
            description = "Apple OAuth 로그인 API입니다.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "애플 로그인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResDto.class),
                            examples = @ExampleObject(
                                    name = "success",
                                    value = """
                                {
                                  "status": 200,
                                  "success": true,
                                  "data": {
                                    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
                                    "newUser": false,
                                    "socialType": "APPLE"
                                  }
                                }
                                """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "애플 로그인 실패",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "APPLE_LOGIN_FAILED",
                                            value = """
                                        {
                                          "status": 400,
                                          "success": false,
                                          "error": {
                                            "code": "APPLE_LOGIN_FAILED",
                                            "message": "애플 로그인에 실패했습니다."
                                          }
                                        }
                                        """
                                    ),
                                    @ExampleObject(
                                            name = "APPLE_PROFILE_REQUEST_FAILED",
                                            value = """
                                        {
                                          "status": 400,
                                          "success": false,
                                          "error": {
                                            "code": "APPLE_PROFILE_REQUEST_FAILED",
                                            "message": "애플 사용자 정보 조회에 실패했습니다."
                                          }
                                        }
                                        """
                                    )
                            }
                    )
            )
    })
    LoginResDto appleLogin(AppleLoginReqDto reqDto);

    @Operation(
            summary = "게스트 로그인",
            description = "회원가입 없이 임시 게스트 계정을 생성하거나, 기존 게스트 ID로 로그인하여 토큰을 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게스트 로그인 성공",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "신규 게스트 생성",
                                    summary = "최초 진입 시",
                                    value = "{\"status\": 200, \"success\": true, \"data\": {\"accessToken\": \"ey...\", \"refreshToken\": \"ey...\", \"newUser\": true, \"socialType\": \"GUEST\"}}"
                            ),
                            @ExampleObject(
                                    name = "기존 게스트 로그인",
                                    summary = "guestUserId 보유 시",
                                    value = "{\"status\": 200, \"success\": true, \"data\": {\"accessToken\": \"ey...\", \"refreshToken\": \"ey...\", \"newUser\": false, \"socialType\": \"GUEST\"}}"
                            )
                    })
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(examples = @ExampleObject(
                            name = "사용자 없음",
                            value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"USER_NOT_FOUND\", \"message\": \"사용자를 찾을 수 없습니다.\"}}"
                    ))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "권한 부족",
                    content = @Content(examples = @ExampleObject(
                            name = "권한 위반",
                            summary = "게스트 계정이 아닌 ID로 접근 시",
                            value = "{\"status\": 403, \"success\": false, \"data\": {\"errorClassName\": \"INVALID_USER_ROLE\", \"message\": \"해당 작업을 수행할 수 있는 권한이 없습니다.\"}}"
                    ))
            )
    })
    GuestLoginResDto guestLogin(GuestLoginReqDto reqDto);

    @Operation(
            summary = "Access / Refresh 토큰 재발급",
            description = "만료된 Access Token 대신 Refresh Token을 이용해 토큰을 재발급합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 재발급 성공"),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않거나 만료된 리프레시 토큰",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 401, \"success\": false, \"data\": {\"errorClassName\": \"INVALID_REFRESH_TOKEN\", \"message\": \"유효하지 않은 리프레시 토큰입니다.\"}}"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효성 검사 실패",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"VALIDATION_ERROR\", \"message\": \"{refreshToken=리프레시 토큰이 누락되었습니다.}\"}}"))
            )
    })
    RefreshResDto refresh(RefreshReqDto reqDto);

    @Operation(
            summary = "로그아웃",
            description = "현재 로그인한 사용자의 리프레시 토큰을 삭제하여 로그아웃합니다."
    )
    @ApiResponse(
            responseCode = "200",
            description = "로그아웃 성공",
            content = @Content(examples = @ExampleObject(value = "{\"status\": 200, \"success\": true, \"data\": {\"message\": \"성공적으로 로그아웃 되었습니다.\"}}"))
    )
    MessageResDto logout(CustomUserDetails user);
}