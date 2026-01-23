package com.example.ForDay.domain.user.controller;

import com.example.ForDay.domain.user.dto.request.NicknameRegisterReqDto;
import com.example.ForDay.domain.user.dto.response.NicknameCheckResDto;
import com.example.ForDay.domain.user.dto.response.NicknameRegisterResDto;
import com.example.ForDay.domain.user.dto.response.UserInfoResDto;
import com.example.ForDay.global.oauth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Tag(name = "User", description = "사용자 프로필 및 계정 관련 API")
public interface UserControllerDocs {

    @Operation(
            summary = "닉네임 중복 체크",
            description = "닉네임 형식 검증 및 중복 여부를 확인합니다. (한글/영문/숫자, 최대 10자)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "중복 체크 요청 성공 (사용 가능 또는 중복됨)",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "사용 가능한 경우",
                                    value = "{\"status\": 200, \"success\": true, \"data\": {\"nickname\": \"포비123\", \"message\": \"사용 가능한 닉네임입니다.\", \"available\": true}}"
                            ),
                            @ExampleObject(
                                    name = "이미 사용 중인 경우",
                                    value = "{\"status\": 200, \"success\": true, \"data\": {\"nickname\": \"포비123\", \"message\": \"이미 사용 중인 닉네임입니다.\", \"available\": false}}"
                            )
                    })
            )
    })
    NicknameCheckResDto nicknameCheck(
            @Parameter(description = "중복 확인할 닉네임", example = "포비123") String nickname
    );

    @Operation(
            summary = "닉네임 등록",
            description = "닉네임 형식 검증 후 등록합니다. (한글/영문/숫자, 최대 10자, 중복 불가)"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "닉네임 등록 성공",
                    content = @Content(examples = @ExampleObject(
                            value = "{\"status\": 200, \"success\": true, \"data\": {\"nickname\": \"포비123\", \"message\": \"닉네임이 성공적으로 설정되었습니다.\"}}"
                    ))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효성 검사 실패 또는 중복된 닉네임",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "유효성 검사 실패",
                                    summary = "형식 오류",
                                    value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"VALIDATION_ERROR\", \"message\": \"{nickname=닉네임은 필수입니다.}\"}}"
                            ),
                            @ExampleObject(
                                    name = "닉네임 중복",
                                    summary = "이미 존재하는 닉네임",
                                    value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"USERNAME_ALREADY_EXISTS\", \"message\": \"이미 사용 중인 사용자 이름입니다.\"}}"
                            )
                    })
            )
    })
    NicknameRegisterResDto nicknameRegister(NicknameRegisterReqDto reqDto, CustomUserDetails user);

    @Operation(
            summary = "사용자 정보 조회",
            description = "마이페이지 등에 표시될 사용자의 닉네임, 프로필 이미지, 총 스티커 개수를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public UserInfoResDto getUserInfo(@AuthenticationPrincipal CustomUserDetails user);
}