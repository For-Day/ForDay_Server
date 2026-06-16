package com.example.ForDay.domain.record.controller.v3;

import com.example.ForDay.domain.record.dto.response.GetRecordDetailResDtoV3;
import com.example.ForDay.global.oauth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v3/records")
public class ActivityRecordControllerV3 implements ActivityRecordControllerV3Docs {

    @Override
    @GetMapping("/{recordId}")
    public GetRecordDetailResDtoV3 getRecordDetail(@PathVariable(name = "recordId") Long recordId,
                                                   @AuthenticationPrincipal CustomUserDetails user) {
        return null; // TODO: 서비스 로직 구현 예정
    }

    // 기록 수정

    // 기록 삭제
}
