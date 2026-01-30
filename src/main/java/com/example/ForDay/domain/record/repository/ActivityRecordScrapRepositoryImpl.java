package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.QActivityRecord;
import com.example.ForDay.domain.record.entity.QActivityRecordScarp;
import com.example.ForDay.domain.user.dto.response.GetUserScrapListResDto;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ActivityRecordScrapRepositoryImpl implements ActivityRecordScrapRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private QActivityRecord record;
    private QActivityRecordScarp scrap;

    @Override
    public List<GetUserScrapListResDto.ScrapDto> getMyScrapList(Long lastScrapId, Integer size, String targetUserId) {
        return queryFactory
                .select(Projections.constructor(
                        GetUserScrapListResDto.ScrapDto.class,
                        record.id,
                        record.imageUrl,
                        record.sticker,
                        record.memo,
                        scrap.createdAt
                ))
                .from(scrap)
                .join(scrap.activityRecord, record)
                .where(
                        ltLastScrapId(lastScrapId),
                        scrap.user.id.eq(targetUserId)
                )
                .orderBy(scrap.id.desc())
                .limit(size + 1)
                .fetch();
    }

    private BooleanExpression ltLastScrapId(Long lastScrapId) {
        return lastScrapId != null ? scrap.id.lt(lastScrapId) : null;
    }
}
