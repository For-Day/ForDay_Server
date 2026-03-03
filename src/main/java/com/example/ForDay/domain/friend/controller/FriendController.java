package com.example.ForDay.domain.friend.controller;

import com.example.ForDay.domain.friend.dto.request.AddFriendReqDto;
import com.example.ForDay.domain.friend.dto.request.BlockFriendReqDto;
import com.example.ForDay.domain.friend.dto.request.ReportFriendReqDto;
import com.example.ForDay.domain.friend.dto.response.*;
import com.example.ForDay.domain.friend.service.FriendService;
import com.example.ForDay.global.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/friends")
public class FriendController implements FriendControllerDocs {
    private final FriendService friendService;

    @Override
    @PostMapping
    public AddFriendResDto addFriend(@RequestBody @Valid AddFriendReqDto reqDto,
                                     @AuthenticationPrincipal CustomUserDetails user) {
        return friendService.addFriend(reqDto, user);
    }

    @Override
    @DeleteMapping("/{friendId}")
    public DeleteFriendResDto deleteFriend(@RequestParam(name = "friendId") String friendId,
                                           @AuthenticationPrincipal CustomUserDetails user) {
        return friendService.deleteFriend(friendId, user);
    }

    @Override
    @PostMapping("/block")
    public BlockFriendResDto blockFriend(@RequestBody @Valid BlockFriendReqDto reqDto,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        return friendService.blockFriend(reqDto, user);
    }

    @PostMapping("/report")
    public ReportFriendResDto reportFriend(@RequestBody @Valid ReportFriendReqDto reqDto,
                                           @AuthenticationPrincipal CustomUserDetails user) {
        return friendService.reportFriend(reqDto, user);
    }

    @Override
    @GetMapping
    public GetFriendListResDto getFriendList(@RequestParam(name = "lastUserId", required = false) String lastUserId,
                                             @RequestParam(name = "size", required = false, defaultValue = "20") Integer size,
                                             @AuthenticationPrincipal CustomUserDetails user) {
        return friendService.getFriendList(user, lastUserId, size);
    }
}
