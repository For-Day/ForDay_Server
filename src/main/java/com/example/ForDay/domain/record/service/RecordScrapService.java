package com.example.ForDay.domain.record.service;

import com.example.ForDay.domain.record.dto.ActivityRecordWithUserDto;
import com.example.ForDay.domain.record.dto.response.AddActivityRecordScrapResDto;
import com.example.ForDay.domain.record.dto.response.DeleteActivityRecordScrapResDto;
import com.example.ForDay.domain.record.entity.ActivityRecordScrap;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.record.repository.ActivityRecordScrapRepository;
import com.example.ForDay.domain.record.utils.ActivityRecordUtil;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.oauth.CustomUserDetails;
import com.example.ForDay.global.util.UserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordScrapService {
    private final ActivityRecordRepository activityRecordRepository;
    private final UserUtil userUtil;
    private final ActivityRecordScrapRepository activityRecordScrapRepository;
    private final ActivityRecordUtil activityRecordUtil;
    @Transactional
    public AddActivityRecordScrapResDto addActivityRecordScrap(Long recordId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        getAccessibleRecordWithUser(recordId, currentUser);
        validateDuplicateScrap(recordId, currentUser.getId());

        ActivityRecordScrap scrap = ActivityRecordScrap.of(activityRecordRepository.getReferenceById(recordId), currentUser);
        activityRecordScrapRepository.save(scrap);

        return AddActivityRecordScrapResDto.from(recordId);
    }

    @Transactional
    public DeleteActivityRecordScrapResDto deleteActivityRecordScrap(Long recordId, CustomUserDetails user) {
        User currentUser = userUtil.getCurrentUser(user);
        Optional<ActivityRecordScrap> scrap = activityRecordScrapRepository.findByActivityRecordIdAndUserId(recordId, currentUser.getId());

        if (scrap.isEmpty()) {
            return DeleteActivityRecordScrapResDto.notExistScrap(recordId);
        }
        activityRecordScrapRepository.delete(scrap.get());

        return DeleteActivityRecordScrapResDto.deleteScrap(recordId);
    }

    private void validateDuplicateScrap(Long recordId, String userId) {
        if (activityRecordScrapRepository.existsByScrap(recordId, userId)) {
            throw new CustomException(ErrorCode.DUPLICATE_SCRAP);
        }
    }

    private ActivityRecordWithUserDto getAccessibleRecordWithUser(Long recordId, User user) {
        ActivityRecordWithUserDto record = activityRecordRepository.getActivityRecordWithUser(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));

        activityRecordUtil.validateAccess(user.getId(), record.getWriterId(), record.isWriterDeleted(), record.getVisibility());

        return record;
    }

}
