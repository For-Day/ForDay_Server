package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.activity.entity.QActivity;
import com.example.ForDay.domain.hobby.dto.response.GetStickerInfoResDto;
import com.example.ForDay.domain.hobby.entity.QHobby;
import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.entity.QActivityRecord;
import com.example.ForDay.domain.user.dto.response.GetUserFeedListResDto;
import com.example.ForDay.domain.user.entity.QUser;
import com.example.ForDay.domain.user.entity.User;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ActivityRecordRepositoryImpl implements ActivityRecordRepositoryCustom {
    private final JPAQueryFactory queryFactory;

    private final QActivityRecord record = QActivityRecord.activityRecord;
    private final QUser user = QUser.user;
    private final QActivity activity = QActivity.activity;

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


    @Override
    public List<GetUserFeedListResDto.FeedDto> findUserFeedList(List<Long> hobbyIds, Long lastRecordId, Integer feedSize, String userId) {
        return queryFactory
                .select(Projections.constructor(
                        GetUserFeedListResDto.FeedDto.class,
                        record.id,
                        record.imageUrl,
                        record.sticker,
                        record.memo,
                        record.createdAt
                ))
                .from(record)
                .where(
                        record.user.id.eq(userId),
                        ltLastRecordId(lastRecordId),
                        hobbyIdIn(hobbyIds)
                )
                .orderBy(record.id.desc())
                .limit(feedSize + 1)
                .fetch();
    }

    @Override
    public Optional<RecordDetailQueryDto> findDetailDtoById(Long recordId) {
        return Optional.ofNullable(queryFactory
                .select(Projections.constructor(RecordDetailQueryDto.class,
                        record.id,
                        record.imageUrl,
                        record.memo,
                        record.sticker,
                        record.createdAt,
                        record.visibility,
                        user.id,
                        user.nickname,
                        user.profileImageUrl,
                        activity.content
                ))
                .from(record)
                .join(record.user, user)
                .join(record.activity, activity)
                .where(record.id.eq(recordId))
                .fetchOne());
    }

    @Override
    public Long countRecordByHobbyIds(List<Long> hobbyIds, String userId) {
        Long count = queryFactory
                .select(record.count())
                .from(record)
                .where(
                        record.user.id.eq(userId),
                        hobbyIdIn(hobbyIds)
                )
                .fetchOne();

        return count == null ? 0L : count;
    }

    private BooleanExpression hobbyIdIn(List<Long> hobbyIds) {
        if (hobbyIds == null || hobbyIds.isEmpty()) return null;
        return record.hobby.id.in(hobbyIds);
    }



    private BooleanExpression ltLastRecordId(Long lastRecordId) {
        return lastRecordId != null ? record.id.lt(lastRecordId) : null;
    }
}
