package com.example.ForDay.domain.hobby.service.v2;

import com.example.ForDay.domain.hobby.dto.AiInsightResult;
import com.example.ForDay.domain.hobby.dto.request.HobbyCreateReqDtoV2;
import com.example.ForDay.domain.hobby.dto.request.UpdateMyHobbySettingReqDtoV2;
import com.example.ForDay.domain.hobby.dto.response.GetHomeHobbyInfoResDto;
import com.example.ForDay.domain.hobby.dto.response.HobbyCreateResDtoV2;
import com.example.ForDay.domain.hobby.dto.response.MyHobbySettingResDtoV2;
import com.example.ForDay.domain.hobby.dto.response.UpdateMyHobbySettingResDtoV2;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.service.HobbyAiInsightService;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.hobby.utils.HobbyUtil;
import com.example.ForDay.domain.hobby.validator.HobbyValidator;
import com.example.ForDay.domain.notification.service.NotificationService;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HobbyServiceV2 {
    private final HobbyRepository hobbyRepository;
    private final HobbyValidator hobbyValidator;
    private final HobbyUtil hobbyUtil;
    private final NotificationService notificationService;
    private final HobbyAiInsightService hobbyAiInsightService;

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

    @Transactional
    public UpdateMyHobbySettingResDtoV2 updateMyHobbySetting(UpdateMyHobbySettingReqDtoV2 reqDto, User currentUser) {
        List<Hobby> userHobbies = hobbyRepository.findAllByUser(currentUser);
        Map<Long, Hobby> hobbyMap = userHobbies.stream().collect(Collectors.toMap(Hobby::getId, h -> h));

        hobbyValidator.validateDuplicateSequence(reqDto.getProgressHobbyList());
        for (UpdateMyHobbySettingReqDtoV2.ProgressUpdateInfo info : reqDto.getProgressHobbyList()) {
            Hobby hobby = hobbyMap.get(info.getHobbyId());
            if (hobby == null) throw new CustomException(ErrorCode.HOBBY_NOT_FOUND);

            hobby.updateStatusAndSequence(info.getSequence(), HobbyStatus.IN_PROGRESS);
        }

        if (reqDto.getHiddenHobbyList() != null) {
            hobbyValidator.validateDuplicateSequence(reqDto.getHiddenHobbyList());
            for (UpdateMyHobbySettingReqDtoV2.HiddenUpdateInfo info : reqDto.getHiddenHobbyList()) {
                Hobby hobby = hobbyMap.get(info.getHobbyId());
                if (hobby == null) throw new CustomException(ErrorCode.HOBBY_NOT_FOUND);

                hobby.updateStatusAndSequence(info.getSequence(), HobbyStatus.ARCHIVED);
            }
        }

        return UpdateMyHobbySettingResDtoV2.from(userHobbies);
    }

    @Transactional(readOnly = true)
    public GetHomeHobbyInfoResDto getHomeHobbyInfo(Long hobbyId, User currentUser) {
        log.info("[GetHomeHobbyInfoV2] Dashboard inquiry - UserId: {}, TargetHobbyId: {}", currentUser.getId(), hobbyId == null ? "DEFAULT(Latest)" : hobbyId);

        Hobby targetHobby = (hobbyId != null) ? hobbyUtil.getHobby(hobbyId) : hobbyUtil.getFirstHobby(currentUser);

        if (targetHobby == null) {
            return GetHomeHobbyInfoResDto.ofDefault(currentUser.getNickname());
        }

        GetHomeHobbyInfoResDto response = hobbyRepository.getHomeHobbyInfoV2(targetHobby.getId(), currentUser);
        if (response == null) {
            log.warn("[GetHomeHobbyInfoV2] Failed to fetch hobby data - HobbyId: {}", targetHobby.getId());
            return GetHomeHobbyInfoResDto.ofDefault(currentUser.getNickname());
        }

        AiInsightResult aiInsight = hobbyAiInsightService.resolveInsight(currentUser, targetHobby);
        log.info("[GetHomeHobbyInfoV2] Completion - UserId: {}, Hobby: {}, AI Success: {}", currentUser.getId(), targetHobby.getHobbyName(), !aiInsight.summaryText().isEmpty());

        return GetHomeHobbyInfoResDto.of(notificationService.unreadNotificationExists(currentUser), response, currentUser.getNickname(), aiInsight);
    }

    private int getNextStartSequence(User user) {
        Integer maxSeq = hobbyRepository.findMaxSequenceByUserAndStatus(user, HobbyStatus.IN_PROGRESS);
        return (maxSeq == null) ? 1 : maxSeq + 1;
    }
}
