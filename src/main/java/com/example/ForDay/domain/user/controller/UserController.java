package com.example.ForDay.domain.user.controller;

import com.example.ForDay.domain.user.dto.response.NicknameCheckResDto;
import com.example.ForDay.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final UserService userService;

    @Operation(
            summary = "닉네임 중복 체크",
            description = "닉네임 형식 검증 및 중복 여부를 확인합니다. (한글/영문/숫자, 최대 10자)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "정상적으로 처리됨"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    @GetMapping("/nickname/availability")
    public NicknameCheckResDto nicknameCheck(
            @Parameter(description = "중복 확인할 닉네임", example = "지나123")
            @RequestParam String nickname
    ) {
        return userService.nicknameCheck(nickname);
    }
}
