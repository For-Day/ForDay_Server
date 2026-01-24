package com.example.ForDay.domain.hobby.repository;

import com.example.ForDay.domain.user.dto.response.GetUserHobbyCardListResDto;
import com.example.ForDay.domain.user.entity.User;

import java.util.List;

public interface HobbyCardRepositoryCustom {
    List<GetUserHobbyCardListResDto.HobbyCardDto> findUserHobbyCardList(Long lastHobbyCardId, Integer size, User currentUser);
}
