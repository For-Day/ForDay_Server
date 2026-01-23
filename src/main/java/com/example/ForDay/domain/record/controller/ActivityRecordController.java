package com.example.ForDay.domain.record.controller;

import com.example.ForDay.domain.record.dto.response.GetRecordDetailResDto;
import com.example.ForDay.domain.record.service.ActivityRecordService;
import com.example.ForDay.global.oauth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/records")
public class ActivityRecordController {
    private final ActivityRecordService activityRecordService;

    @GetMapping("/{activityRecordId}")
    public GetRecordDetailResDto getRecordDetail(@PathVariable(name = "activityRecordId") Long activityRecordId, @AuthenticationPrincipal CustomUserDetails user) {
        return activityRecordService.getRecordDetail(activityRecordId, user);
    }
}
