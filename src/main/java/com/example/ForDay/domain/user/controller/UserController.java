package com.example.ForDay.domain.user.controller;

import com.example.ForDay.domain.user.dto.request.NicknameRegisterReqDto;
import com.example.ForDay.domain.user.dto.request.SetUserProfileImageReqDto;
import com.example.ForDay.domain.user.dto.response.*;
import com.example.ForDay.domain.user.service.UserService;
import com.example.ForDay.global.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController implements UserControllerDocs {
    private final UserService userService;

    @Override
    @GetMapping("/nickname/availability")
    public NicknameCheckResDto nicknameCheck(@RequestParam String nickname) {
        return userService.nicknameCheck(nickname);
    }

    @Override
    @PatchMapping("/nickname")
    public NicknameRegisterResDto nicknameRegister(
            @RequestBody @Valid NicknameRegisterReqDto reqDto,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return userService.nicknameRegister(reqDto.getNickname(), user);
    }

    @Override
    @GetMapping("/info")
    public UserInfoResDto getUserInfo(@AuthenticationPrincipal CustomUserDetails user) {
        return userService.getUserInfo(user);
    }

    @Override
    @PatchMapping("/profile-image")
    public SetUserProfileImageResDto setUserProfileImage(@RequestBody @Valid SetUserProfileImageReqDto reqDto, @AuthenticationPrincipal CustomUserDetails user) {
        return userService.setUserProfileImage(reqDto, user);
    }

    @Override
    @GetMapping("/hobbies/in-progress")
    public GetHobbyInProgressResDto getHobbyInProgress(@AuthenticationPrincipal CustomUserDetails user) {
        return userService.getHobbyInProgress(user);
    }

}