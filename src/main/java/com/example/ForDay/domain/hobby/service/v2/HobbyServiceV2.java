package com.example.ForDay.domain.hobby.service.v2;

import com.example.ForDay.domain.hobby.dto.request.*;
import com.example.ForDay.domain.hobby.dto.response.*;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.validator.HobbyValidator;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
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
public class HobbyServiceV2 {
    private final HobbyRepository hobbyRepository;
    private final HobbyValidator hobbyValidator;

    @Transactional
    public HobbyCreateResDtoV2 hobbyCreate(HobbyCreateReqDtoV2 reqDto, User currentUser) {
        log.info("[HobbyCreate] Start - userId={}, hobbyCount={}", currentUser.getId(), reqDto.getHobbyList().size());

        hobbyValidator.validateMaxInProgressHobbiesV2(currentUser, reqDto.getHobbyList().size());
        hobbyValidator.validateDuplicateHobbyV2(reqDto.getHobbyList(), currentUser);

        List<Hobby> hobbies = reqDto.getHobbyList().stream()
                .map(info -> Hobby.createNewHobbyV2(currentUser, info))
                .toList();

        List<Hobby> savedHobbies = hobbyRepository.saveAll(hobbies);

        if (!currentUser.isOnboardingCompleted()) {
            currentUser.completeOnboarding();
            log.info("[HobbyCreate] User onboarding marked as completed: userId={}", currentUser.getId());
        }

        return HobbyCreateResDtoV2.from(savedHobbies);
    }

    public MyHobbySettingResDtoV2 myHobbySetting(User currentUser) {
    }
}
