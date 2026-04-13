package com.example.ForDay.domain.hobby.service.v2;

import com.example.ForDay.domain.hobby.dto.request.*;
import com.example.ForDay.domain.hobby.dto.response.*;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
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

import java.util.ArrayList;
import java.util.Comparator;
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

        int nextStartSeq = getNextStartSequence(currentUser);

        List<Hobby> hobbies = new ArrayList<>();
        for (int i = 0; i < reqDto.getHobbyList().size(); i++) {
            hobbies.add(Hobby.createNewHobbyV2(currentUser, reqDto.getHobbyList().get(i), nextStartSeq + i));
        }

        List<Hobby> savedHobbies = hobbyRepository.saveAll(hobbies);

        if (!currentUser.isOnboardingCompleted()) {
            currentUser.completeOnboarding();
            log.info("[HobbyCreate] User onboarding marked as completed: userId={}", currentUser.getId());
        }

        return HobbyCreateResDtoV2.from(savedHobbies);
    }

    @Transactional(readOnly = true)
    public MyHobbySettingResDtoV2 myHobbySetting(User currentUser) {
        List<Hobby> allHobbies = hobbyRepository.findAllByUser(currentUser);

        List<MyHobbySettingResDtoV2.ProgressHobbyList> progressList = allHobbies.stream()
                .filter(h -> h.getStatus() == HobbyStatus.IN_PROGRESS)
                .sorted(Comparator.comparing(Hobby::getSequence, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(MyHobbySettingResDtoV2.ProgressHobbyList::from)
                .toList();

        List<MyHobbySettingResDtoV2.HiddenHobbyList> hiddenList = allHobbies.stream()
                .filter(h -> h.getStatus() == HobbyStatus.ARCHIVED)
                .sorted(Comparator.comparing(Hobby::getCreatedAt).reversed())
                .map(MyHobbySettingResDtoV2.HiddenHobbyList::from)
                .toList();

        return new MyHobbySettingResDtoV2(progressList, hiddenList);
    }

    private int getNextStartSequence(User user) {
        Integer maxSeq = hobbyRepository.findMaxSequenceByUserAndStatus(user, HobbyStatus.IN_PROGRESS);
        return (maxSeq == null) ? 1 : maxSeq + 1;
    }
}
