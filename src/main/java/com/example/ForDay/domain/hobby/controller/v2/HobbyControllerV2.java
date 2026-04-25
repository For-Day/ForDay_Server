package com.example.ForDay.domain.hobby.controller.v2;

import com.example.ForDay.domain.hobby.dto.request.HobbyCreateReqDtoV2;
import com.example.ForDay.domain.hobby.dto.request.UpdateMyHobbySettingReqDtoV2;
import com.example.ForDay.domain.hobby.dto.response.*;
import com.example.ForDay.domain.hobby.service.v1.HobbyService;
import com.example.ForDay.domain.hobby.service.v2.HobbyServiceV2;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.global.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v2/hobbies")
public class HobbyControllerV2 implements HobbyControllerV2Docs {
    private final HobbyServiceV2 hobbyServiceV2;

    @Override
    @PostMapping("/create")
    public HobbyCreateResDtoV2 hobbyCreate(@RequestBody @Valid HobbyCreateReqDtoV2 reqDto,
                                           @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyServiceV2.hobbyCreate(reqDto, user.getUser());
    }

    @Override
    @GetMapping("/setting")
    public MyHobbySettingResDtoV2 myHobbySetting(@AuthenticationPrincipal CustomUserDetails user) {
        return hobbyServiceV2.myHobbySetting(user.getUser());
    }

    @Override
    @PutMapping("/setting")
    public UpdateMyHobbySettingResDtoV2 updateMyHobbySetting(@RequestBody @Valid UpdateMyHobbySettingReqDtoV2 reqDto,
                                                             @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyServiceV2.updateMyHobbySetting(reqDto, user.getUser());
    }

    @GetMapping("/home")
    public GetHomeHobbyInfoResDto getHomeHobbyInfo(@RequestParam(value = "hobbyId", required = false) Long hobbyId,
                                                   @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyServiceV2.getHomeHobbyInfo(hobbyId, user.getUser());
    }
}
