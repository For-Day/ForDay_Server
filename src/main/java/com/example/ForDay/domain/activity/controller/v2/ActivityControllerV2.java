/*
package com.example.ForDay.domain.activity.controller.v2;

import com.example.ForDay.domain.activity.controller.v1.ActivityControllerDocs;
import com.example.ForDay.domain.activity.dto.request.RecordActivityReqDtoV2;
import com.example.ForDay.domain.activity.dto.request.UpdateActivityReqDto;
import com.example.ForDay.domain.activity.dto.response.GetAiRecommendItemsResDto;
import com.example.ForDay.domain.activity.service.ActivityRecommendItemService;
import com.example.ForDay.domain.activity.service.ActivityService;
import com.example.ForDay.domain.activity.type.AIItemType;
import com.example.ForDay.domain.hobby.dto.request.RecordActivityReqDto;
import com.example.ForDay.domain.hobby.dto.response.RecordActivityResDto;
import com.example.ForDay.domain.record.service.v2.ActivityRecordServiceV2;
import com.example.ForDay.global.common.response.dto.MessageResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/activities")
public class ActivityControllerV2  {
    private final ActivityRecordServiceV2 recordServiceV2;

    @PostMapping("/record") // C (기록 생성)
    public RecordActivityResDto recordActivity(@RequestBody @Valid RecordActivityReqDtoV2 reqDto,
                                               @AuthenticationPrincipal CustomUserDetails user) {
        return recordServiceV2.recordActivity(reqDto, user.getUser());
    }

    @GetMapping("/record") // R (기록 읽기)
    public RecordActivityResDto recordActivity(@AuthenticationPrincipal CustomUserDetails user) {
        return recordServiceV2.recordActivity(user.getUser());
    }

    @PutMapping("/record") // U (기록 수정)
    public RecordActivityResDto recordActivity(@RequestBody @Valid RecordActivityReqDtoV2 reqDto,
                                               @AuthenticationPrincipal CustomUserDetails user) {
        return recordServiceV2.recordActivity(reqDto, user.getUser());
    }
    @DeleteMapping("/record") // D (기록 삭제)
    public RecordActivityResDto recordActivity(@RequestBody @Valid RecordActivityReqDtoV2 reqDto,
                                               @AuthenticationPrincipal CustomUserDetails user) {
        return recordServiceV2.recordActivity(reqDto, user.getUser());
    }
}
*/
