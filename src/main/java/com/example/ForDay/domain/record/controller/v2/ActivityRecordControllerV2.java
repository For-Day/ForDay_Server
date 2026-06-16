package com.example.ForDay.domain.record.controller.v2;

import com.example.ForDay.domain.reaction.service.ReactionService;
import com.example.ForDay.domain.record.dto.request.ActivityRecordReqDtoV2;
import com.example.ForDay.domain.record.dto.request.ReactToRecordReqDto;
import com.example.ForDay.domain.record.dto.request.RecordSearchConditionReqDto;
import com.example.ForDay.domain.record.dto.response.*;
import com.example.ForDay.domain.record.service.v2.ActivityRecordServiceV2;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.global.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/records")
public class ActivityRecordControllerV2 implements ActivityRecordControllerV2Docs{
    private final ActivityRecordServiceV2 activityRecordServiceV2;
    private final ReactionService reactionService;

    @Override
    @GetMapping("/{recordId}")
    public GetRecordDetailResDtoV2 getRecordDetailV2(@PathVariable(name = "recordId") Long recordId,
                                                     @Valid @ModelAttribute RecordSearchConditionReqDto condition,
                                                     @AuthenticationPrincipal CustomUserDetails user,
                                                     @RequestParam(name = "hobbyIds", required = false) List<Long> hobbyIds,
                                                     @RequestParam(name = "notificationId", required = false) Long notificationId) {
        return activityRecordServiceV2.getRecordDetailV2(recordId, condition, user, hobbyIds, notificationId);
    }

    // 기록에 반응한 유저 목록 조회 (최초 조회 -> 전체 + 감정별 목록 한번에 조회)
    @Override
    @GetMapping("/{recordId}/reactions/summary")
    public ReactionSummaryResDto getReactionSummary(@PathVariable Long recordId,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @AuthenticationPrincipal CustomUserDetails user
    ) {
        return reactionService.getReactionSummary(recordId, size, user);
    }

    // 무한 스크롤 (탭별 조회)
    @Override
    @GetMapping("/{recordId}/reactions")
    public ReactionTabScrollResDto getReactionTabScroll(@PathVariable Long recordId,
                                                @RequestParam(required = false) RecordReactionType type,
                                                @RequestParam(required = false) Long lastReactionId,
                                                @RequestParam(defaultValue = "10") int size,
                                                @AuthenticationPrincipal CustomUserDetails user
    ) {
        return reactionService.getReactionTabScroll(recordId, type, lastReactionId, size, user);
    }

    @PostMapping("/{recordId}/reaction")
    public ReactToRecordResDto reactToRecord(@PathVariable(name = "recordId") Long recordId,
                                             @RequestBody ReactToRecordReqDto reqDto,
                                             @AuthenticationPrincipal CustomUserDetails user) {
        return activityRecordServiceV2.reactToRecordWithRedis(recordId, reqDto.getReactionType(), user);
    }

    // 활동 기록
    @PostMapping
    public ActivityRecordResDto recordActivity(@RequestBody @Valid ActivityRecordReqDtoV2 reqDto,
                                               @AuthenticationPrincipal CustomUserDetails user) {
        return activityRecordServiceV2.recordActivity(reqDto, user);
    }

    // 기록 수정

    // 기록 삭제

    // 기록 조회

    // 취미별 자주 사용하는 memo 조회
}
