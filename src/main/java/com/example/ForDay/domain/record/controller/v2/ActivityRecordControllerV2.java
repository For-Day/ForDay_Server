package com.example.ForDay.domain.record.controller.v2;

import com.example.ForDay.domain.record.dto.request.RecordSearchConditionReqDto;
import com.example.ForDay.domain.record.dto.response.GetRecordDetailResDtoV2;
import com.example.ForDay.domain.record.service.v2.ActivityRecordServiceV2;
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

    @GetMapping("/{recordId}")
    public GetRecordDetailResDtoV2 getRecordDetailV2(@PathVariable(name = "recordId") Long recordId,
                                                     @Valid @ModelAttribute RecordSearchConditionReqDto condition,
                                                     @AuthenticationPrincipal CustomUserDetails user,
                                                     @RequestParam(name = "hobbyIds", required = false) List<Long> hobbyIds) {
        return activityRecordServiceV2.getRecordDetailV2(recordId, condition, user, hobbyIds);
    }
}
