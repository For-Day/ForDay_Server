package com.example.ForDay.domain.hobby.repository;

import com.example.ForDay.domain.auth.dto.response.OnboardingDataDto;
import com.example.ForDay.domain.hobby.dto.response.GetHomeHobbyInfoResDto;
import com.example.ForDay.domain.hobby.dto.response.MyHobbySettingResDto;
import com.example.ForDay.domain.hobby.dto.response.ReCheckHobbyInfoResDto;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.dto.response.GetHobbyInProgressResDto;
import com.example.ForDay.domain.user.entity.User;

import java.util.List;

public interface HobbyRepositoryCustom {
    GetHomeHobbyInfoResDto getHomeHobbyInfo(Long hobbyId, User currentUser);
    MyHobbySettingResDto myHobbySetting(User user, HobbyStatus hobbyStatus);
    OnboardingDataDto getOnboardingDate(User user);
    List<GetHobbyInProgressResDto.HobbyDto> findUserTabHobbyList(User currentUser);

    List<ReCheckHobbyInfoResDto.HobbyInfoDto> reCheckHobbyInfo(String currentUserId);
}
