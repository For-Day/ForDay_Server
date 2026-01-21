package com.example.ForDay.domain.activity.repository;

import com.example.ForDay.domain.activity.entity.QActivityRecord;
import com.example.ForDay.domain.hobby.dto.response.GetStickerInfoResDto;
import com.example.ForDay.domain.user.entity.User;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ActivityRecordRepositoryImpl implements ActivityRecordRepositoryCustom{
    private final JPAQueryFactory queryFactory;
    private QActivityRecord activityRecord = QActivityRecord.activityRecord;

    @Override
    public GetStickerInfoResDto getStickerInfo(Long hobbyId, Integer page, Integer size, User currentUser) {
        return null;
    }
}
