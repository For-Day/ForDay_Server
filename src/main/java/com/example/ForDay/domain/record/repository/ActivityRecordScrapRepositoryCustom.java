package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.user.dto.response.GetUserScrapListResDto;

import java.util.List;

public interface ActivityRecordScrapRepositoryCustom {

    List<GetUserScrapListResDto.ScrapDto> getMyScrapList(Long lastScrapId, Integer size, String targetUserId);

    List<GetUserScrapListResDto.ScrapDto> getOtherScrapList(Long lastScrapId, Integer size, String targetUserId, String id, List<String> myFriendIds, List<String> blockFriendIds);
}
