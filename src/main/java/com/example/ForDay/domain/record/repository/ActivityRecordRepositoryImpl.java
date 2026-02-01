package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.activity.entity.QActivity;
import com.example.ForDay.domain.hobby.dto.response.GetStickerInfoResDto;
import com.example.ForDay.domain.record.dto.ActivityRecordWithUserDto;
import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.dto.ReportActivityRecordDto;
import com.example.ForDay.domain.record.dto.response.GetActivityRecordByStoryResDto;
import com.example.ForDay.domain.record.entity.QActivityRecord;
import com.example.ForDay.domain.record.entity.QActivityRecordReaction;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.dto.response.GetUserFeedListResDto;
import com.example.ForDay.domain.user.entity.QUser;
import com.example.ForDay.domain.user.entity.User;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
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
    private final QActivityRecordReaction reaction = QActivityRecordReaction.activityRecordReaction;

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
                        record.sticker,
                        record.deleted
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
    public List<GetUserFeedListResDto.FeedDto> findUserFeedList(List<Long> hobbyIds, Long lastRecordId, Integer feedSize, String userId, List<RecordVisibility> visibilities) {
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
                        hobbyIdIn(hobbyIds),
                        record.visibility.in(visibilities),
                        record.deleted.isFalse() // 삭제 안된 기록만 조회
                )
                .orderBy(record.id.desc())
                .limit(feedSize + 1)
                .fetch();
    }

    @Override
    public Optional<RecordDetailQueryDto> findDetailDtoById(Long recordId) {
        return Optional.ofNullable(queryFactory
                .select(Projections.constructor(RecordDetailQueryDto.class,
                        record.hobby.id,
                        record.activity.id,
                        record.id,
                        record.imageUrl,
                        record.memo,
                        record.sticker,
                        record.createdAt,
                        record.visibility,
                        user.id,
                        user.nickname,
                        user.profileImageUrl,
                        user.deleted,
                        activity.content,
                        record.deleted
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

    @Override
    public Optional<ActivityRecordWithUserDto> getActivityRecordWithUser(Long recordId) {
        return Optional.ofNullable(
                queryFactory
                        .select(Projections.constructor(ActivityRecordWithUserDto.class,
                                record.id,
                                record.visibility,
                                record.user.id,
                                record.user.deleted
                        ))
                        .from(record)
                        .join(record.user) // 명시적 조인 추가 (필요 시)
                        .where(record.id.eq(recordId))
                        .fetchOne()
        );
    }

    @Override
    public Optional<ReportActivityRecordDto> getReportActivityRecord(Long recordId) {
        return Optional.ofNullable(
                queryFactory
                        .select(Projections.constructor(ReportActivityRecordDto.class,
                                record.id,
                                record,
                                record.user,
                                record.user.id,
                                record.user.deleted,
                                record.user.nickname,
                                record.visibility
                        ))
                        .from(record)
                        .join(record.user)
                        .where(record.id.eq(recordId))
                        .fetchOne()
        );
    }

    @Override
    public List<GetActivityRecordByStoryResDto.RecordDto> getActivityRecordByStory(
            Long hobbyInfoId, Long lastRecordId, Integer size, String keyword,
            String currentUserId, List<String> myFriendIds, List<String> blockFriendIds) {

        return queryFactory
                .select(Projections.constructor(GetActivityRecordByStoryResDto.RecordDto.class,
                        record.id,
                        record.imageUrl,
                        record.sticker,
                        record.activity.content,
                        record.memo,
                        Projections.constructor(GetActivityRecordByStoryResDto.UserInfoDto.class,
                                record.user.id,
                                record.user.nickname,
                                record.user.profileImageUrl
                        ),
                        JPAExpressions
                                .select(reaction.count().gt(0L))
                                .from(reaction)
                                .where(reaction.activityRecord.id.eq(record.id)
                                        .and(reaction.reactedUser.id.eq(currentUserId)))
                ))
                .from(record)
                .join(record.activity, activity)
                .join(record.user, user)
                .where(
                        record.hobby.hobbyInfoId.eq(hobbyInfoId),      // 1. 대상 취미 필터링
                        record.user.id.ne(currentUserId),       // 2. 자신의 기록 제외
                        ltLastRecordId(lastRecordId),           // 3. No-offset 페이징
                        record.user.deleted.isFalse(),          // 4. 탈퇴한 유저 제외
                        notInBlockList(blockFriendIds),         // 5. 차단 관계 유저 제외 (방어 로직)
                        containsKeyword(keyword),               // 6. 키워드 검색 추가
                        // 7. 공개 범위 및 본인/친구 권한 체크
                        record.visibility.eq(RecordVisibility.PUBLIC)
                                .or(
                                        record.visibility.eq(RecordVisibility.FRIEND)
                                                .and(record.user.id.in(myFriendIds))
                                ),
                        record.deleted.isFalse() // 8. 삭제되지 않은 기록만 조회
                )
                .orderBy(record.id.desc())
                .limit(size)
                .fetch();
    }

    private BooleanExpression notInBlockList(List<String> blockFriendIds) {
        return (blockFriendIds == null || blockFriendIds.isEmpty())
                ? null : record.user.id.notIn(blockFriendIds);
    }

    private BooleanExpression containsKeyword(String keyword) {
        return (keyword == null || keyword.isBlank())
                ? null : record.activity.content.contains(keyword)  // 활동 내용
                .or(record.memo.contains(keyword));  // 활동 기록 메모
    }

    private BooleanExpression hobbyIdIn(List<Long> hobbyIds) {
        if (hobbyIds == null || hobbyIds.isEmpty()) return null;
        return record.hobby.id.in(hobbyIds);
    }

    private BooleanExpression ltLastRecordId(Long lastRecordId) {
        return lastRecordId != null ? record.id.lt(lastRecordId) : null;
    }
}
