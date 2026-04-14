package com.example.ForDay.domain.hobby.controller.v2;

import com.example.ForDay.domain.hobby.dto.request.HobbyCreateReqDtoV2;
import com.example.ForDay.domain.hobby.dto.request.UpdateMyHobbySettingReqDtoV2;
import com.example.ForDay.domain.hobby.dto.response.HobbyCreateResDtoV2;
import com.example.ForDay.domain.hobby.dto.response.MyHobbySettingResDto;
import com.example.ForDay.domain.hobby.dto.response.MyHobbySettingResDtoV2;
import com.example.ForDay.domain.hobby.dto.response.UpdateMyHobbySettingResDtoV2;
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

    // 취미 설정 조회에 이미지 코드 추가 반환
    @Override
    @PostMapping("/create")
    public HobbyCreateResDtoV2 hobbyCreate(@RequestBody @Valid HobbyCreateReqDtoV2 reqDto,
                                           @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyServiceV2.hobbyCreate(reqDto, user.getUser());
    }

    @GetMapping("/setting")
    public MyHobbySettingResDtoV2 myHobbySetting(@AuthenticationPrincipal CustomUserDetails user) {
        return hobbyServiceV2.myHobbySetting(user.getUser());
    }

    @PutMapping("/setting")
    public UpdateMyHobbySettingResDtoV2 updateMyHobbySetting(@RequestBody @Valid UpdateMyHobbySettingReqDtoV2 reqDto,
                                                             @AuthenticationPrincipal CustomUserDetails user) {
        return hobbyServiceV2.updateMyHobbySetting(reqDto, user.getUser());
    }
}
