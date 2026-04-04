package com.example.ForDay.domain.activity.service;

import com.example.ForDay.domain.activity.entity.OtherActivity;
import com.example.ForDay.domain.activity.repository.OtherActivityRepository;
import com.example.ForDay.domain.hobby.dto.response.OthersActivityRecommendResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.utils.HobbyUtil;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtherActivityService {
    private final UserUtil userUtil;
    private final HobbyUtil hobbyUtil;
    private final OtherActivityRepository otherActivityRepository;

    @Transactional(readOnly = true)
    public OthersActivityRecommendResDto othersActivityRecommendV1(Long hobbyId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);

        Hobby hobby = hobbyUtil.getHobbyByUserId(hobbyId, currentUser);

        List<OtherActivity> activities = otherActivityRepository.findRandomThreeByHobbyInfoId(hobby.getHobbyInfoId());

        List<OthersActivityRecommendResDto.ActivityDto> list = activities.stream()
                .map(OthersActivityRecommendResDto.ActivityDto::from)
                .toList();

        return OthersActivityRecommendResDto.of(list);
    }
}
