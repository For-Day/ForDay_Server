package com.example.ForDay.domain.auth.controller;

import com.example.ForDay.domain.auth.dto.KakaoProfileDto;
import com.example.ForDay.domain.auth.dto.request.KakaoLoginReqDto;
import com.example.ForDay.domain.auth.dto.request.RefreshReqDto;
import com.example.ForDay.domain.auth.dto.response.LoginResDto;
import com.example.ForDay.domain.auth.dto.response.RefreshResDto;
import com.example.ForDay.domain.auth.service.AuthService;
import com.example.ForDay.domain.auth.service.KakaoService;
import com.example.ForDay.domain.auth.service.RefreshTokenService;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.service.UserService;
import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Auth", description = "인증 / 로그인 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    @Operation(
            summary = "카카오 로그인"
    )
    @ApiResponse(
            responseCode = "200",
            description = "로그인 성공",
            content = @Content(schema = @Schema(implementation = LoginResDto.class))
    )
    @PostMapping("/kakao")
    public LoginResDto kakaoLogin(@RequestBody @Valid KakaoLoginReqDto reqDto) {
        return authService.kakaoLogin(reqDto);
    }

    @PostMapping("/guest")
    @Operation(
            summary = "게스트 로그인",
            description = "회원가입 없이 임시 게스트 계정을 생성하고 Access/Refresh 토큰을 발급합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "게스트 로그인 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResDto.class)
                    )
            )
    })
    public LoginResDto guestLogin() {
        return authService.guestLogin();
    }


    @PostMapping("/refresh")
    @Operation(
            summary = "Access / Refresh 토큰 재발급",
            description = "만료된 Access Token 대신 Refresh Token을 이용해 토큰을 재발급합니다."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "토큰 재발급 성공",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RefreshResDto.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 리프레시 토큰",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                {
                                  "status": 401,
                                  "success": false,
                                  "data": {
                                    "errorClassName": "INVALID_REFRESH_TOKEN",
                                    "message": "유효하지 않은 리프레시 토큰입니다."
                                  }
                                }
                                """
                            )
                    )
            )
    })
    public RefreshResDto refresh(@RequestBody @Valid RefreshReqDto reqDto) {
        return authService.refresh(reqDto);
    }
}
