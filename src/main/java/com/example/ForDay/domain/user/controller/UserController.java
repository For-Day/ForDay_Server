package com.example.ForDay.domain.user.controller;

import com.example.ForDay.domain.user.dto.request.NicknameRegisterReqDto;
import com.example.ForDay.domain.user.dto.response.NicknameCheckResDto;
import com.example.ForDay.domain.user.dto.response.NicknameRegisterResDto;
import com.example.ForDay.domain.user.service.UserService;
import com.example.ForDay.global.oauth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
            @Parameter(description = "중복 확인할 닉네임", example = "포비123")
            @RequestParam String nickname
    ) {
        return userService.nicknameCheck(nickname);
    }

    @PatchMapping("/nickname")
    @Operation(
            summary = "닉네임 등록",
            description = """
        닉네임 형식 검증 후 등록합니다. 
        
        ✔ 한글/영문/숫자만 가능  
        ✔ 최대 10자  
        ✔ 중복 불가
        """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "닉네임 등록 성공"),
            @ApiResponse(responseCode = "400", description = "유효성 검사 실패"),
            @ApiResponse(responseCode = "409", description = "이미 사용 중인 닉네임"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public NicknameRegisterResDto nicknameRegister(@RequestBody @Valid NicknameRegisterReqDto reqDto, @AuthenticationPrincipal CustomUserDetails user) {
        return userService.nicknameRegister(reqDto.getNickname(), user);
    }
}
