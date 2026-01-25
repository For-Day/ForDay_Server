package com.example.ForDay.domain.activity.repository;

import com.example.ForDay.domain.hobby.dto.response.GetActivityListResDto;
import com.example.ForDay.domain.hobby.dto.response.GetHobbyActivitiesResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.user.entity.User;

public interface ActivityRepositoryCustom {
    GetHobbyActivitiesResDto getHobbyActivities(Long hobbyId, Integer size);
    GetActivityListResDto getActivityList(Long hobbyId, String userId);
}
