package com.example.ForDay.domain.activity.controller;

import com.example.ForDay.domain.activity.dto.request.UpdateActivityReqDto;
import com.example.ForDay.domain.activity.service.ActivityService;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/activities")
public class ActivityController {
    private final ActivityService activityService;

    @PatchMapping("/{activityId}")
    public MessageResDto updateActivity(@PathVariable(name = "activityId") Long activityId,
                                        @RequestBody @Valid UpdateActivityReqDto reqDto,
                                        @AuthenticationPrincipal CustomUserDetails user) {
        return activityService.updateActivity(activityId, reqDto, user);
    }

    @DeleteMapping("/{activityId}")
    public MessageResDto deleteActivity(@PathVariable(name = "activityId") Long activityId,
                                        @AuthenticationPrincipal CustomUserDetails user) {
        return activityService.deleteActivity(activityId, user);
    }
}
