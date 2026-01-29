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

import java.util.List;

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
    public UserInfoResDto getUserInfo(@AuthenticationPrincipal CustomUserDetails user, @RequestParam(name = "userId", required = false) String userId) {
        return userService.getUserInfo(user, userId);
    }

    @Override
    @PatchMapping("/profile-image")
    public SetUserProfileImageResDto setUserProfileImage(@RequestBody @Valid SetUserProfileImageReqDto reqDto, @AuthenticationPrincipal CustomUserDetails user) {
        return userService.setUserProfileImage(reqDto, user);
    }

    @Override
    @GetMapping("/hobbies/in-progress")
    public GetHobbyInProgressResDto getHobbyInProgress(@AuthenticationPrincipal CustomUserDetails user, @RequestParam(name = "userId", required = false) String userId) {
        return userService.getHobbyInProgress(user, userId);
    }

    @Override
    @GetMapping("/feeds")
    public GetUserFeedListResDto getUserFeedList(@RequestParam(name = "hobbyId", required = false) List<Long> hobbyIds,
                                                 @RequestParam(name = "lastRecordId", required = false) Long lastRecordId,
                                                 @RequestParam(name = "feedSize", required = false, defaultValue = "24") Integer feedSize,
                                                 @RequestParam(name = "userId", required = false) String userId,
                                                 @AuthenticationPrincipal CustomUserDetails user) {
        return userService.getUserFeedList(hobbyIds, lastRecordId, feedSize, user, userId);
    }

    @Override
    @GetMapping("/hobby-cards")
    public GetUserHobbyCardListResDto getUserHobbyCardList(@RequestParam(name = "lastHobbyCardId", required = false) Long lastHobbyCardId,
                                                           @RequestParam(name = "size", required = false, defaultValue = "20") Integer size,
                                                           @AuthenticationPrincipal CustomUserDetails user,
                                                           @RequestParam(name = "userId", required = false) String userId) {
        return userService.getUserHobbyCardList(lastHobbyCardId, size, user, userId);
    }

}