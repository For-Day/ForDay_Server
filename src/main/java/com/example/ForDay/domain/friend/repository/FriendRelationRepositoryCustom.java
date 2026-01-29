package com.example.ForDay.domain.friend.repository;

import com.example.ForDay.domain.friend.dto.response.GetFriendListResDto;

import java.util.List;

public interface FriendRelationRepositoryCustom {

    List<GetFriendListResDto.UserInfoDto> findMyFriendList(String id, String lastUserId, Integer size);
}
