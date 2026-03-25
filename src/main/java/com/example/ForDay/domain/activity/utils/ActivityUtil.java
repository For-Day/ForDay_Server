package com.example.ForDay.domain.activity.utils;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.activity.repository.ActivityRepository;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ActivityUtil {
    private final ActivityRepository activityRepository;

    public Activity getActivityByUserId(Long activityId, String userId) {
        return activityRepository.findByIdAndUserId(activityId, userId).orElseThrow(() -> new CustomException(ErrorCode.ACTIVITY_NOT_FOUND));
    }
}
