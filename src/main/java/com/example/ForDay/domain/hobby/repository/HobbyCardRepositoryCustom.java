package com.example.ForDay.domain.hobby.repository;

import com.example.ForDay.domain.user.dto.response.GetUserHobbyCardListResDto;

import java.util.List;

public interface HobbyCardRepositoryCustom {
    List<GetUserHobbyCardListResDto.HobbyCardDto> findUserHobbyCardList(Long lastHobbyCardId, Integer size, String currentUserId);
}
