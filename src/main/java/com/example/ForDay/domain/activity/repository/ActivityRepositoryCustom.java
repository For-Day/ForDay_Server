package com.example.ForDay.domain.activity.repository;

import com.example.ForDay.domain.activity.dto.ActivityRecordCollectInfo;
import com.example.ForDay.domain.hobby.dto.response.GetActivityListResDto;
import com.example.ForDay.domain.hobby.dto.response.GetHobbyActivitiesResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.user.entity.User;

import java.util.Optional;

public interface ActivityRepositoryCustom {
    GetHobbyActivitiesResDto getHobbyActivities(Long hobbyId, Integer size);
    GetActivityListResDto getActivityList(Long hobbyId, String userId);

    Optional<ActivityRecordCollectInfo> getCollectActivityInfo(Long activityId);
}
