package com.example.ForDay.domain.activity.repository;

import com.example.ForDay.domain.hobby.dto.response.GetHobbyActivitiesResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;

public interface ActivityRepositoryCustom {
    GetHobbyActivitiesResDto getHobbyActivities(Hobby hobby);
}
