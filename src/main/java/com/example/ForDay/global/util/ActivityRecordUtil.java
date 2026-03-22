package com.example.ForDay.global.util;

import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityRecordUtil {
    private final ActivityRecordRepository activityRecordRepository;

    public ActivityRecord getRecord(Long recordId) {
        return activityRecordRepository.findById(recordId)
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));
    }

    public ActivityRecord getRecordByUserId(Long recordId, User user) {
        return activityRecordRepository.findByIdAndUserId(recordId, user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_RECORD_NOT_FOUND));
    }
}
