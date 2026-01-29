package com.example.ForDay.domain.friend.controller;

import com.example.ForDay.domain.friend.dto.request.AddFriendReqDto;
import com.example.ForDay.domain.friend.dto.response.AddFriendResDto;
import com.example.ForDay.domain.friend.service.FriendService;
import com.example.ForDay.global.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/friends")
public class FriendController {
    private final FriendService friendService;

    @PostMapping
    public AddFriendResDto addFriend(@RequestBody @Valid AddFriendReqDto reqDto,
                                     @AuthenticationPrincipal CustomUserDetails user) {
        return friendService.addFriend(reqDto, user);
    }
}
