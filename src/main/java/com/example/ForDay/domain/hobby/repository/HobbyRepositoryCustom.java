package com.example.ForDay.domain.hobby.repository;

import com.example.ForDay.domain.hobby.dto.response.GetHomeHobbyInfoResDto;
import com.example.ForDay.domain.user.entity.User;

public interface HobbyRepositoryCustom {
    GetHomeHobbyInfoResDto getHomeHobbyInfo(Long hobbyId, User currentUser);
}
