package com.example.ForDay.domain.recent.controller;

import com.example.ForDay.domain.recent.dto.response.DeleteAllRecentKeywordResDto;
import com.example.ForDay.domain.recent.dto.response.DeleteRecentKeywordResDto;
import com.example.ForDay.domain.recent.dto.response.GetRecentKeywordResDto;
import com.example.ForDay.domain.recent.service.RecentService;
import com.example.ForDay.global.oauth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/recent")
public class RecentController {
    private final RecentService recentService;

    @GetMapping
    public GetRecentKeywordResDto getRecentKeyword(@AuthenticationPrincipal CustomUserDetails user) {
        return recentService.getRecentKeyword(user);
    }

    @DeleteMapping
    public DeleteAllRecentKeywordResDto deleteAllRecentKeyword(@AuthenticationPrincipal CustomUserDetails user) {
        return recentService.deleteAllRecentKeyword(user);
    }

    @DeleteMapping("/{recentId}")
    public DeleteRecentKeywordResDto deleteRecentKeyword(@RequestParam(name = "recentId") Long recentId,
                                                         @AuthenticationPrincipal CustomUserDetails user) {
        return recentService.deleteRecentKeyword(recentId, user);
    }
}
