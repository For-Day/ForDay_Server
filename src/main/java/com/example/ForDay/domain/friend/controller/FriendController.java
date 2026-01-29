package com.example.ForDay.domain.friend.controller;

import com.example.ForDay.domain.friend.dto.request.AddFriendReqDto;
import com.example.ForDay.domain.friend.dto.request.BlockFriendReqDto;
import com.example.ForDay.domain.friend.dto.response.AddFriendResDto;
import com.example.ForDay.domain.friend.dto.response.BlockFriendResDto;
import com.example.ForDay.domain.friend.dto.response.DeleteFriendResDto;
import com.example.ForDay.domain.friend.dto.response.GetFriendListResDto;
import com.example.ForDay.domain.friend.service.FriendService;
import com.example.ForDay.global.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    @DeleteMapping("/{friendId}")
    public DeleteFriendResDto deleteFriend(@RequestParam(name = "friendId") String friendId,
                                           @AuthenticationPrincipal CustomUserDetails user) {
        return friendService.deleteFriend(friendId, user);
    }

    @PostMapping("/block")
    public BlockFriendResDto blockFriend(@RequestBody @Valid BlockFriendReqDto reqDto,
                                         @AuthenticationPrincipal CustomUserDetails user) {
        return friendService.blockFriend(reqDto, user);
    }

    @GetMapping
    public GetFriendListResDto getFriendList(@RequestParam(name = "lastUserId", required = false) String lastUserId,
                                             @RequestParam(name = "size", required = false, defaultValue = "20") Integer size,
                                             @AuthenticationPrincipal CustomUserDetails user) {
        return friendService.getFriendList(user, lastUserId, size);
    }
}
