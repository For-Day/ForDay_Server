package com.example.ForDay.domain.record.controller.v2;

import com.example.ForDay.domain.reaction.service.RecordReactionService;
import com.example.ForDay.domain.record.dto.request.RecordSearchConditionReqDto;
import com.example.ForDay.domain.record.dto.response.GetRecordDetailResDtoV2;
import com.example.ForDay.domain.record.dto.response.ReactionListResDto;
import com.example.ForDay.domain.record.dto.response.ReactionSummaryResDto;
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
public class ActivityRecordControllerV2 {
    private final ActivityRecordServiceV2 activityRecordServiceV2;
    private final RecordReactionService recordReactionService;

    @GetMapping("/{recordId}")
    public GetRecordDetailResDtoV2 getRecordDetailV2(@PathVariable(name = "recordId") Long recordId,
                                                     @Valid @ModelAttribute RecordSearchConditionReqDto condition,
                                                     @AuthenticationPrincipal CustomUserDetails user,
                                                     @RequestParam(name = "hobbyIds", required = false) List<Long> hobbyIds) {
        return activityRecordServiceV2.getRecordDetailV2(recordId, condition, user, hobbyIds);
    }

    // 기록에 반응한 유저 목록 조회 (최초 조회 -> 전체 + 감정별 목록 한번에 조회)
    @GetMapping("/{recordId}/reactions/summary")
    public ReactionSummaryResDto getReactionSummary(@PathVariable Long recordId,
                                                    @RequestParam(defaultValue = "20") int size,
                                                    @AuthenticationPrincipal CustomUserDetails user
    ) {
        return recordReactionService.getReactionSummary(recordId, size, user);
    }

    // 무한 스크롤 (탭별 조회)
    @GetMapping("/{recordId}/reactions")
    public ReactionListResDto getReactions(@PathVariable Long recordId,
                                           @RequestParam(required = false) RecordReactionType type, // null이면 ALL
                                           @RequestParam(required = false) String cursor, // encoded cursor
                                           @RequestParam(defaultValue = "10") int size,
                                           @AuthenticationPrincipal CustomUserDetails user
    ) {
        return recordReactionService.getReactions(recordId, type, cursor, size, user);
    }
}
