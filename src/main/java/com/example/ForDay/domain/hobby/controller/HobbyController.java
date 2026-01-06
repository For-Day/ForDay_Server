package com.example.ForDay.domain.hobby.controller;

import com.example.ForDay.domain.hobby.dto.request.ActivityCreateReqDto;
import com.example.ForDay.domain.hobby.dto.response.ActivityCreateResDto;
import com.example.ForDay.domain.hobby.service.HobbyService;
import com.example.ForDay.global.oauth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/hobbies")
public class HobbyController {
    private final HobbyService hobbyService;

    @PostMapping("/activities/create")
    @Operation(
            summary = "취미 루틴 생성",
            description = "사용자의 취미 정보로 루틴을 생성합니다."
    )
    public ActivityCreateResDto hobbyCreate(@RequestBody @Valid ActivityCreateReqDto reqDto, @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyService.hobbyCreate(reqDto, user);
    }
}
