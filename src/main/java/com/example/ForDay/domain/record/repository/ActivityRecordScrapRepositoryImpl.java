package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.QActivityRecord;
import com.example.ForDay.domain.record.entity.QActivityRecordScarp;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.dto.response.GetUserScrapListResDto;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ActivityRecordScrapRepositoryImpl implements ActivityRecordScrapRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final QActivityRecord record = QActivityRecord.activityRecord;
    private final QActivityRecordScarp scrap = QActivityRecordScarp.activityRecordScarp;

    @Override
    public List<GetUserScrapListResDto.ScrapDto> getMyScrapList(Long lastScrapId, Integer size, String targetUserId) {
        return queryFactory
                .select(Projections.constructor(
                        GetUserScrapListResDto.ScrapDto.class,
                        scrap.id,
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

    public List<GetUserScrapListResDto.ScrapDto> getOtherScrapList(Long lastScrapId, Integer size, String targetUserId, String currentUserId, List<String> myFriendIds) {
        return queryFactory
                .select(Projections.constructor(GetUserScrapListResDto.ScrapDto.class,
                        scrap.id,
                        record.id,
                        record.imageUrl,
                        record.sticker,
                        record.memo,
                        scrap.createdAt
                ))
                .from(scrap)
                .join(scrap.activityRecord, record)
                .where(
                        scrap.user.id.eq(targetUserId),
                        ltLastScrapId(lastScrapId),
                        record.visibility.eq(RecordVisibility.PUBLIC)
                                .or(record.visibility.eq(RecordVisibility.FRIEND)
                                        .and(record.user.id.in(myFriendIds).or(record.user.id.eq(currentUserId))))
                )
                .orderBy(scrap.id.desc())
                .limit(size + 1)
                .fetch();
    }

    private BooleanExpression ltLastScrapId(Long lastScrapId) {
        return lastScrapId != null ? scrap.id.lt(lastScrapId) : null;
    }
}
