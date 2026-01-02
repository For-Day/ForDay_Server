package com.example.ForDay.domain.auth.controller;

import com.example.ForDay.domain.auth.dto.KakaoProfileDto;
import com.example.ForDay.domain.auth.dto.request.GuestLoginReqDto;
import com.example.ForDay.domain.auth.dto.request.KakaoLoginReqDto;
import com.example.ForDay.domain.auth.dto.request.RefreshReqDto;
import com.example.ForDay.domain.auth.dto.response.LoginResDto;
import com.example.ForDay.domain.auth.dto.response.RefreshResDto;
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
    private final KakaoService kakaoService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

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
        // accessToken을 활용하여 카카오 사용자 정보 얻기
        KakaoProfileDto kakaoProfileDto = kakaoService.getKakaoProfile(reqDto.getKakaoAccessToken());

        boolean isNewUser = false;
        // 회원가입이 되어 있지 않다면 회원가입
        User originalUser = userService.getUserBySocialId(kakaoProfileDto.getId());
        if(originalUser == null) {
            isNewUser = true;
            // 회원가입
            originalUser = userService.createOauth(kakaoProfileDto.getId(), kakaoProfileDto.getKakao_account(), SocialType.KAKAO);
        }

        // 회원 가입 되어 있는 경우 -> 토큰 발급
        String accessToken = jwtUtil.createAccessToken(originalUser.getSocialId(), Role.USER);
        String refreshToken = jwtUtil.createRefreshToken(originalUser.getSocialId());

        refreshTokenService.save(originalUser.getSocialId(), refreshToken);

        return new LoginResDto(accessToken, refreshToken, isNewUser, SocialType.KAKAO);

    }

    @PostMapping("/guest")
    public LoginResDto guestLogin(@RequestBody @Valid GuestLoginReqDto reqDto) {

        return null;
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

        String refreshToken = reqDto.getRefreshToken();

        // 리프레시 토큰 유효성 검사
        if (!jwtUtil.validate(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String username = jwtUtil.getUsername(refreshToken);

        // 저장된 refreshToken 조회
        String storedToken = refreshTokenService.get(username);

        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 토큰 재발급
        Role role = userService.getRoleByUsername(username);

        String newAccessToken = jwtUtil.createAccessToken(username, role);
        String newRefreshToken = jwtUtil.createRefreshToken(username);

        refreshTokenService.save(username, newRefreshToken);

        return new RefreshResDto(newAccessToken, newRefreshToken);
    }
}
