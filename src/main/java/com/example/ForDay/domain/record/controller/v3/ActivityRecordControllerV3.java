package com.example.ForDay.domain.record.controller.v3;

import com.example.ForDay.domain.record.dto.request.RecordSearchConditionReqDto;
import com.example.ForDay.domain.record.dto.response.GetRecordDetailResDtoV3;
import com.example.ForDay.domain.record.service.v3.ActivityRecordServiceV3;
import com.example.ForDay.global.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v3/records")
public class ActivityRecordControllerV3 implements ActivityRecordControllerV3Docs {

    private final ActivityRecordServiceV3 activityRecordServiceV3;

    @Override
    @GetMapping("/{recordId}")
    public GetRecordDetailResDtoV3 getRecordDetail(@PathVariable(name = "recordId") Long recordId,
                                                   @Valid @ModelAttribute RecordSearchConditionReqDto condition,
                                                   @AuthenticationPrincipal CustomUserDetails user,
                                                   @RequestParam(name = "hobbyIds", required = false) List<Long> hobbyIds,
                                                   @RequestParam(name = "notificationId", required = false) Long notificationId) {
        return activityRecordServiceV3.getRecordDetail(recordId, condition, user, hobbyIds, notificationId);
    }
}
