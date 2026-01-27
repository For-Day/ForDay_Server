package com.example.ForDay.domain.auth.controller;

import com.example.ForDay.domain.auth.dto.request.*;
import com.example.ForDay.domain.auth.dto.response.*;
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

    @PostMapping("/apple")
    public LoginResDto appleLogin(@RequestBody @Valid AppleLoginReqDto reqDto) {
        log.info("[LOGIN] Apple login request received");
        return authService.appleLogin(reqDto);
    }

    @Override
    @PostMapping("/guest")
    public GuestLoginResDto guestLogin(@RequestBody GuestLoginReqDto reqDto) {
        log.info("[LOGIN] guest login request received");
        return authService.guestLogin(reqDto);
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

    @Override
    @GetMapping("/validate")
    public TokenValidateResDto tokenValidate() {
        return authService.tokenValidate();
    }

    @PatchMapping("/switch-account")
    public SwitchAccountResDto switchAccount(@RequestBody @Valid SwitchAccountReqDto reqDto, @AuthenticationPrincipal CustomUserDetails user) {
        return authService.switchAccount(reqDto, user);
    }
}