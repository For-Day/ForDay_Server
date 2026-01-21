package com.example.ForDay.domain.activity.repository;

import com.example.ForDay.domain.hobby.dto.response.GetStickerInfoResDto;
import com.example.ForDay.domain.user.entity.User;

import java.util.List;

public interface ActivityRecordRepositoryCustom {
    List<GetStickerInfoResDto.StickerDto> getStickerInfo(Long hobbyId, Integer page, Integer size, User currentUser);
}
