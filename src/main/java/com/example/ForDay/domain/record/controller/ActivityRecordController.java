package com.example.ForDay.domain.record.controller;

import com.example.ForDay.domain.record.dto.request.UpdateRecordVisibilityReqDto;
import com.example.ForDay.domain.record.dto.response.GetRecordDetailResDto;
import com.example.ForDay.domain.record.dto.response.UpdateRecordVisibilityResDto;
import com.example.ForDay.domain.record.service.ActivityRecordService;
import com.example.ForDay.global.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/records")
public class ActivityRecordController implements ActivityRecordControllerDocs{
    private final ActivityRecordService activityRecordService;

    @Override
    @GetMapping("/{activityRecordId}")
    public GetRecordDetailResDto getRecordDetail(@PathVariable(name = "activityRecordId") Long activityRecordId, @AuthenticationPrincipal CustomUserDetails user) {
        return activityRecordService.getRecordDetail(activityRecordId, user);
    }

    @Override
    @PatchMapping("/{activityRecordId}/visibility")
    public UpdateRecordVisibilityResDto updateRecordVisibility(@PathVariable(name = "activityRecordId") Long activityRecordId,
                                                               @RequestBody @Valid UpdateRecordVisibilityReqDto reqDto,
                                                               @AuthenticationPrincipal CustomUserDetails user) {
        return activityRecordService.updateRecordVisibility(activityRecordId, reqDto, user);
    }
}
