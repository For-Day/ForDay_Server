package com.example.ForDay.domain.hobby.validator;

import com.example.ForDay.domain.hobby.dto.request.HobbyCreateReqDto;
import com.example.ForDay.domain.hobby.dto.request.HobbyCreateReqDtoV2;
import com.example.ForDay.domain.hobby.dto.request.UpdateMyHobbySettingReqDtoV2;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.hobby.repository.HobbyRepository;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.hobby.utils.HobbyUtil;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class HobbyValidator {
    private final HobbyRepository hobbyRepository;
    private final HobbyUtil hobbyUtil;

    public Hobby validateHobbyAccess(Long hobbyId, User user) {
        Hobby hobby = hobbyUtil.getHobby(hobbyId);
        hobbyUtil.verifyHobbyOwner(hobby, user);
        hobby.validateInProgress();
        return hobby;
    }

    public void validateDuplicateHobby(HobbyCreateReqDto reqDto, User user) {
        if (reqDto.getHobbyInfoId() != null && reqDto.getHobbyInfoId() >= 1) {
            if (hobbyRepository.existsByHobbyInfoIdAndUserId(reqDto.getHobbyInfoId(), user.getId())) {
                throw new CustomException(ErrorCode.ALREADY_HAVE_HOBBY);
            }
        }
        if (StringUtils.hasText(reqDto.getHobbyName())) {
            if (hobbyRepository.existsByHobbyNameAndUserId(reqDto.getHobbyName(), user.getId())) {
                throw new CustomException(ErrorCode.ALREADY_HAVE_HOBBY);
            }
        }
    }

    public void validateMaxInProgressHobbies(User user) {
        long hobbyCount = hobbyRepository.countByStatusAndUser(HobbyStatus.IN_PROGRESS, user);
        if (hobbyCount >= 2) {
            throw new CustomException(ErrorCode.MAX_IN_PROGRESS_HOBBY_EXCEEDED);
        }
    }

    public void validateMaxInProgressHobbiesV2(User currentUser, int addHobbyCount) {
        // 현재 진행 중인 취미 갯수 + 새로 추가하는 취미 갯수 > 10 이면 예외 발생
        long inProgressHobbyCount = hobbyRepository.countByStatusAndUser(HobbyStatus.IN_PROGRESS, currentUser);
        if(inProgressHobbyCount + addHobbyCount > 10) {
            throw new CustomException(ErrorCode.MAX_HOBBY_EXCEEDED);
        }

    }

    public void validateDuplicateHobbyV2(List<HobbyCreateReqDtoV2.HobbyInfo> hobbyList, User currentUser) {
        if (hobbyList == null || hobbyList.isEmpty()) return;

        List<Long> hobbyInfoIds = hobbyList.stream()
                .map(HobbyCreateReqDtoV2.HobbyInfo::getHobbyInfoId)
                .filter(id -> id != null && id >= 1)
                .distinct()
                .toList();

        List<String> hobbyNames = hobbyList.stream()
                .map(HobbyCreateReqDtoV2.HobbyInfo::getHobbyName)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        String userId = currentUser.getId();

        // ID 중복 체크
        if (!hobbyInfoIds.isEmpty()) {
            if (hobbyRepository.existsByUserIdAndHobbyInfoIdIn(userId, hobbyInfoIds)) {
                throw new CustomException(ErrorCode.ALREADY_HAVE_HOBBY);
            }
        }

        // 이름 중복 체크
        if (!hobbyNames.isEmpty()) {
            if (hobbyRepository.existsByUserIdAndHobbyNameIn(userId, hobbyNames)) {
                throw new CustomException(ErrorCode.ALREADY_HAVE_HOBBY);
            }
        }
    }

    public void validateDuplicateSequence(List<UpdateMyHobbySettingReqDtoV2.ProgressUpdateInfo> progressList) {
        long distinctCount = progressList.stream()
                .map(UpdateMyHobbySettingReqDtoV2.ProgressUpdateInfo::getSequence)
                .distinct()
                .count();

        if (distinctCount != progressList.size()) {
            throw new CustomException(ErrorCode.DUPLICATE_SEQUENCE);
        }
    }
}
