package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.hobby.dto.response.GetStickerInfoResDto;
import com.example.ForDay.domain.hobby.entity.QHobby;
import com.example.ForDay.domain.record.entity.QActivityRecord;
import com.example.ForDay.domain.user.entity.User;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ActivityRecordRepositoryImpl implements ActivityRecordRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    private final QActivityRecord record = QActivityRecord.activityRecord;
    private final QHobby hobby = QHobby.hobby;

    @Override
    public List<GetStickerInfoResDto.StickerDto> getStickerInfo(
            Long hobbyId,
            Integer currentPage,
            Integer size,
            User user
    ) {
        QActivityRecord record = QActivityRecord.activityRecord;

        int offset = (currentPage - 1) * size;

        return queryFactory
                .select(Projections.constructor(
                        GetStickerInfoResDto.StickerDto.class,
                        record.id,
                        record.sticker
                ))
                .from(record)
                .where(
                        record.hobby.id.eq(hobbyId),
                        record.user.eq(user)
                )
                .orderBy(record.createdAt.asc())
                .offset(offset)
                .limit(size)
                .fetch();
    }
}
