package com.example.ForDay.domain.user.controller;

import com.example.ForDay.domain.user.dto.request.NicknameRegisterReqDto;
import com.example.ForDay.domain.user.dto.request.SetUserProfileImageReqDto;
import com.example.ForDay.domain.user.dto.response.*;
import com.example.ForDay.global.oauth.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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
            description = "마이페이지 등에 표시될 사용자의 닉네임, 프로필 이미지, 총 스티커 개수를 조회합니다. 자신의 정보 조회시 userId는 null, 다른 사용자 정보 조회시 해당 사용자 userId를 넣습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(
                            name = "USER_NOT_FOUND",
                            summary = "존재하지 않는 사용자",
                            value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"USER_NOT_FOUND\", \"message\": \"사용자를 찾을 수 없습니다.\"}}"
                    ))
            )
    })
    UserInfoResDto getUserInfo(@AuthenticationPrincipal CustomUserDetails user,  @RequestParam(name = "userId", required = false) String userId);

    @Operation(
            summary = "프로필 이미지 변경",
            description = "S3에 업로드된 URL을 받아 사용자의 프로필 이미지를 업데이트합니다. 기존 이미지와 동일할 경우 변경되지 않습니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "성공 (신규 등록 또는 동일 이미지)",
                    content = @Content(schema = @Schema(implementation = SetUserProfileImageResDto.class),
                            examples = {
                                    @ExampleObject(name = "신규 등록 성공", value = "{\"status\": 200, \"success\": true, \"data\": {\"profileImageUrl\": \"https://forday-s3...\", \"message\": \"사용자의 프로필 이미지 URL이 정상적으로 등록되었습니다.\"}}"),
                                    @ExampleObject(name = "동일 이미지 기존 등록", value = "{\"status\": 200, \"success\": true, \"data\": {\"profileImageUrl\": \"https://forday-s3...\", \"message\": \"이미 동일한 프로필 이미지로 설정되어 있습니다.\"}}")
                            })
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "S3 파일 없음",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"S3_IMAGE_NOT_FOUND\", \"message\": \"S3에 해당 이미지가 존재하지 않습니다. 업로드 여부를 확인해주세요.\"}}"))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "유효성 검사 실패",
                    content = @Content(examples = @ExampleObject(value = "{\"status\": 400, \"success\": false, \"data\": {\"errorClassName\": \"VALIDATION_ERROR\", \"message\": \"{profileImageUrl=올바른 URL 형식이어야 합니다.}\"}}"))
            )
    })
    SetUserProfileImageResDto setUserProfileImage(@RequestBody @Valid SetUserProfileImageReqDto reqDto, @AuthenticationPrincipal CustomUserDetails user);

    @Operation(
            summary = "유저 상단 취미 탭 조회",
            description = "현재 로그인한 사용자의 진행 중인 취미 개수, 전체 취미 카드 개수, 그리고 취미 리스트(진행 중 우선 정렬)를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    useReturnTypeSchema = true // GetHobbyInProgressResDto 구조 자동 반영
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(
                            name = "USER_NOT_FOUND",
                            summary = "존재하지 않는 사용자",
                            value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"USER_NOT_FOUND\", \"message\": \"사용자를 찾을 수 없습니다.\"}}"
                    ))
            )
    })
    GetHobbyInProgressResDto getHobbyInProgress(@AuthenticationPrincipal CustomUserDetails user, @RequestParam(name = "userId", required = false) String userId);


    @Operation(
            summary = "활동 피드 목록 조회",
            description = "사용자의 활동 기록을 최신순으로 조회합니다. 특정 취미 필터링 및 무한 스크롤(No-offset) 페이징을 지원합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공 (기록이 없는 경우 totalFeedCount가 null일 수 있음)",
                    useReturnTypeSchema = true
            )
    })
    @GetMapping("/feeds")
    public GetUserFeedListResDto getUserFeedList(
            @Parameter(description = "필터링할 취미 ID 목록 (입력하지 않으면 (빈리스트이면) 전체 피드 조회)", example = "1, 2, 3")
            @RequestParam(name = "hobbyId", required = false) List<Long> hobbyIds,

            @Parameter(description = "마지막으로 조회된 기록 ID (첫 페이지 요청 시에는 비워둠)", example = "455")
            @RequestParam(name = "lastRecordId", required = false) Long lastRecordId,

            @Parameter(description = "한 번에 가져올 피드 개수 (기본값: 24)", example = "24")
            @RequestParam(name = "feedSize", required = false, defaultValue = "24") Integer feedSize,

            @RequestParam(name = "userId", required = false) String userId,

            @AuthenticationPrincipal CustomUserDetails user);


    @Operation(
            summary = "유저의 취미 카드 리스트 조회",
            description = "사용자가 생성한 취미 카드들을 무한 스크롤 방식으로 조회합니다. 첫 조회 시 lastCardId는 비워둡니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(
                            name = "USER_NOT_FOUND",
                            summary = "존재하지 않는 사용자",
                            value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"USER_NOT_FOUND\", \"message\": \"사용자를 찾을 수 없습니다.\"}}"
                    ))
            )
    })
    GetUserHobbyCardListResDto getUserHobbyCardList(
            @Parameter(description = "마지막으로 조회된 카드의 ID (첫 페이지 조회 시 null)", example = "45")
            @RequestParam(name = "lastCardId", required = false) Long lastHobbyCardId,

            @Parameter(description = "한 번에 가져올 데이터 개수", example = "20")
            @RequestParam(name = "size", required = false, defaultValue = "20") Integer size,

            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(name = "userId", required = false) String userId);

    @Operation(
            summary = "스크랩 목록 조회",
            description = "본인 또는 타인의 스크랩 목록을 조회합니다. 타인 조회 시 권한(공개 범위)에 따라 필터링된 결과가 반환됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "스크랩 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = GetUserScrapListResDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(examples = @ExampleObject(
                            name = "USER_NOT_FOUND",
                            summary = "존재하지 않는 사용자 조회",
                            value = "{\"status\": 404, \"success\": false, \"data\": {\"errorClassName\": \"USER_NOT_FOUND\", \"message\": \"사용자를 찾을 수 없습니다.\"}}"
                    ))
            )
    })
    GetUserScrapListResDto getUserScrapList(
            @Parameter(description = "무한 스크롤 적용을 위한 마지막 조회 scrapId (첫 조회 시 null)", example = "59")
            @RequestParam(name = "lastScrapId", required = false) Long lastScrapId,

            @Parameter(description = "조회하고자 하는 스크랩 개수 (기본값 24)", example = "24")
            @RequestParam(name = "size", required = false, defaultValue = "24") Integer size,

            @AuthenticationPrincipal CustomUserDetails user,

            @Parameter(description = "스크랩을 조회하고자 하는 유저의 ID (null일 경우 본인 조회)", example = "7746f373-4dea-41af-8512-b3a3ad3f2608")
            @RequestParam(name = "userId", required = false) String userId);
}