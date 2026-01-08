package com.example.ForDay.domain.auth.controller;

import com.example.ForDay.domain.auth.dto.request.KakaoLoginReqDto;
import com.example.ForDay.domain.auth.dto.request.RefreshReqDto;
import com.example.ForDay.domain.auth.dto.response.LoginResDto;
import com.example.ForDay.domain.auth.dto.response.RefreshResDto;
import com.example.ForDay.domain.auth.service.AuthService;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController implements AuthControllerDocs {
    private final AuthService authService;

    @Override
    @PostMapping("/kakao")
    public LoginResDto kakaoLogin(@RequestBody @Valid KakaoLoginReqDto reqDto) {
        log.info("[LOGIN] Kakao login request received");
        return authService.kakaoLogin(reqDto);
    }

    @Override
    @PostMapping("/guest")
    public LoginResDto guestLogin() {
        log.info("[LOGIN] guest login request received");
        return authService.guestLogin();
    }

    @Override
    @PostMapping("/refresh")
    public RefreshResDto refresh(@RequestBody @Valid RefreshReqDto reqDto) {
        return authService.refresh(reqDto);
    }

    @Override
    @DeleteMapping("/logout")
    public MessageResDto logout(@AuthenticationPrincipal CustomUserDetails user) {
        log.info("[LOGOUT] 사용자={}", user.getUsername());
        return authService.logout(user);
    }
}