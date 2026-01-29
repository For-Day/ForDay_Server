package com.example.ForDay.domain.friend.controller;

import com.example.ForDay.domain.friend.dto.request.AddFriendReqDto;
import com.example.ForDay.domain.friend.dto.request.BlockFriendReqDto;
import com.example.ForDay.domain.friend.dto.response.AddFriendResDto;
import com.example.ForDay.domain.friend.dto.response.BlockFriendResDto;
import com.example.ForDay.domain.friend.dto.response.DeleteFriendResDto;
import com.example.ForDay.domain.friend.dto.response.GetFriendListResDto;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;


@Tag(name = "Friend", description = "친구 관계 관리 API")
public interface FriendControllerDocs {

    @Operation(
            summary = "친구 추가 (팔로우)",
            description = "상대방의 ID를 이용해 친구 관계를 맺습니다. 상대방이 나를 차단했거나 내가 상대를 차단한 경우 404 에러를 반환하여 보안을 유지합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "친구 추가 성공 또는 이미 완료됨",
                    content = @Content(schema = @Schema(implementation = AddFriendResDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "자기 자신을 추가하려고 시도함",
                    content = @Content(examples = @ExampleObject(value = "{\n  \"status\": 400,\n  \"success\": false,\n  \"data\": {\n    \"errorClassName\": \"CANNOT_FOLLOW_SELF\",\n    \"message\": \"자기 자신에게 친구 맺기를 할 수 없습니다.\"\n  }\n}"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음 (존재하지 않거나 차단됨)",
                    content = @Content(examples = @ExampleObject(value = "{\n  \"status\": 404,\n  \"success\": false,\n  \"data\": {\n    \"errorClassName\": \"USER_NOT_FOUND\",\n    \"message\": \"사용자를 찾을 수 없습니다.\"\n  }\n}"))
            )
    })
    AddFriendResDto addFriend(AddFriendReqDto reqDto, CustomUserDetails user);

    @Operation(
            summary = "친구 삭제 (언팔로우)",
            description = "상대방의 ID를 이용해 맺고 있는 친구 관계를 끊습니다. 상대방이 나를 차단했거나 실제 친구 관계가 아닐 경우 에러를 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "친구 삭제 성공",
                    content = @Content(schema = @Schema(implementation = DeleteFriendResDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "삭제 실패 (존재하지 않는 유저 혹은 친구 관계가 아님)",
                    content = @Content(examples = {
                            @ExampleObject(
                                    name = "사용자를 찾을 수 없음",
                                    value = "{\n  \"status\": 404,\n  \"success\": false,\n  \"data\": {\n    \"errorClassName\": \"USER_NOT_FOUND\",\n    \"message\": \"사용자를 찾을 수 없습니다.\"\n  }\n}"
                            ),
                            @ExampleObject(
                                    name = "친구 관계가 아님",
                                    value = "{\n  \"status\": 404,\n  \"success\": false,\n  \"data\": {\n    \"errorClassName\": \"FRIEND_NOT_FOUND\",\n    \"message\": \"존재하지 않는 친구 관계입니다.\"\n  }\n}"
                            )
                    })
            )
    })
    DeleteFriendResDto deleteFriend(@Parameter(description = "친구를 끊고자 하는 유저의 ID", example = "123e4567-e89b-12d3-a456-426614174001") String friendId, CustomUserDetails user );

    @Operation(
            summary = "사용자 차단",
            description = "특정 사용자를 차단합니다. 차단 시 상대방과의 모든 친구 관계가 차단 상태로 변경되며, 나를 차단한 상대나 존재하지 않는 유저는 404 에러를 반환합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "차단 성공 또는 이미 차단됨",
                    content = @Content(schema = @Schema(implementation = BlockFriendResDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "자기 자신을 차단하려고 시도함",
                    content = @Content(examples = @ExampleObject(value = "{\n  \"status\": 400,\n  \"success\": false,\n  \"data\": {\n    \"errorClassName\": \"CANNOT_BLOCK_SELF\",\n    \"message\": \"자기 자신을 차단할 수 없습니다.\"\n  }\n}"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음 (존재하지 않거나 이미 나를 차단한 경우)",
                    content = @Content(examples = @ExampleObject(value = "{\n  \"status\": 404,\n  \"success\": false,\n  \"data\": {\n    \"errorClassName\": \"USER_NOT_FOUND\",\n    \"message\": \"사용자를 찾을 수 없습니다.\"\n  }\n}"))
            )
    })
    BlockFriendResDto blockFriend(@RequestBody @Valid BlockFriendReqDto reqDto, @AuthenticationPrincipal CustomUserDetails user);

    @Operation(
            summary = "친구 목록 조회 (무한 스크롤)",
            description = "내가 팔로우한 친구 목록을 조회합니다. 나를 차단한 유저나 탈퇴한 유저는 목록에서 제외됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "친구 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = GetFriendListResDto.class))
            )
    })
   GetFriendListResDto getFriendList(
            @Parameter(description = "마지막으로 조회된 유저 ID (첫 페이지 조회 시 null)", example = "2cf9d06c-2e27-4b0d-9500-1b4ea56eb4f0")
            @RequestParam(name = "lastUserId", required = false) String lastUserId,
            @Parameter(description = "한 페이지에 조회할 유저 수", example = "20")
            @RequestParam(name = "size", required = false, defaultValue = "20") Integer size,
            @AuthenticationPrincipal CustomUserDetails user);
}
