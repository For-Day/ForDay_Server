package com.example.ForDay.domain.record.controller;

import com.example.ForDay.domain.record.dto.request.ReactToRecordReqDto;
import com.example.ForDay.domain.record.dto.request.UpdateRecordVisibilityReqDto;
import com.example.ForDay.domain.record.dto.response.*;
import com.example.ForDay.domain.record.service.ActivityRecordService;
import com.example.ForDay.domain.record.type.RecordReactionType;
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
    @GetMapping("/{recordId}")
    public GetRecordDetailResDto getRecordDetail(@PathVariable(name = "recordId") Long recordId,
                                                 @AuthenticationPrincipal CustomUserDetails user) {
        return activityRecordService.getRecordDetail(recordId, user);
    }

    @Override
    @PatchMapping("/{recordId}/visibility")
    public UpdateRecordVisibilityResDto updateRecordVisibility(@PathVariable(name = "recordId") Long recordId,
                                                               @RequestBody @Valid UpdateRecordVisibilityReqDto reqDto,
                                                               @AuthenticationPrincipal CustomUserDetails user) {
        return activityRecordService.updateRecordVisibility(recordId, reqDto, user);
    }

    @Override
    @GetMapping("/{recordId}/reaction-users")
    public GetRecordReactionUsersResDto getRecordReactionUsers(@PathVariable(name = "recordId") Long recordId,
                                                               @RequestParam(name = "reactionType") RecordReactionType reactionType,
                                                               @RequestParam(name = "lastUserId", required = false) String lastUserId,
                                                               @RequestParam(name = "size", required = false, defaultValue = "10") Integer size,
                                                               @AuthenticationPrincipal CustomUserDetails user) {
        return activityRecordService.getRecordReactionUsers(recordId, reactionType, user, lastUserId, size);
    }

    @Override
    @PostMapping("/{recordId}/reaction")
    public ReactToRecordResDto reactToRecord(@PathVariable(name = "recordId") Long recordId,
                                             @RequestBody ReactToRecordReqDto reqDto,
                                             @AuthenticationPrincipal CustomUserDetails user) {
        return activityRecordService.reactToRecord(recordId, reqDto.getReactionType(), user);
    }

    @Override
    @DeleteMapping("/{recordId}/reaction")
    public CancelReactToRecordResDto cancelReactToRecord(
            @PathVariable(name = "recordId") Long recordId,
            @RequestParam(name = "reactionType") RecordReactionType reactionType,
            @AuthenticationPrincipal CustomUserDetails user
    ) {
        return activityRecordService.cancelReactToRecord(recordId, reactionType, user);
    }
}
