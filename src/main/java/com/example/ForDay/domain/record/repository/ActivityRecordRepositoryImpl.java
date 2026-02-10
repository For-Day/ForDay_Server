package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.activity.entity.QActivity;
import com.example.ForDay.domain.hobby.dto.response.GetStickerInfoResDto;
import com.example.ForDay.domain.hobby.entity.QHobby;
import com.example.ForDay.domain.record.dto.ActivityRecordWithUserDto;
import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.dto.ReportActivityRecordDto;
import com.example.ForDay.domain.record.dto.response.GetActivityRecordByStoryResDto;
import com.example.ForDay.domain.record.entity.QActivityRecord;
import com.example.ForDay.domain.record.entity.QActivityRecordReaction;
import com.example.ForDay.domain.record.entity.QActivityRecordReport;
import com.example.ForDay.domain.record.service.RedisReactionService;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.record.type.StoryFilterType;
import com.example.ForDay.domain.user.dto.response.GetUserFeedListResDto;
import com.example.ForDay.domain.user.entity.QUser;
import com.example.ForDay.domain.user.entity.User;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ActivityRecordRepositoryImpl implements ActivityRecordRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final RedisReactionService redisReactionService;

    private final QActivityRecord record = QActivityRecord.activityRecord;
    private final QUser user = QUser.user;
    private final QActivity activity = QActivity.activity;
    private final QActivityRecordReaction reaction = QActivityRecordReaction.activityRecordReaction;
    private final QActivityRecordReport activityRecordReport = QActivityRecordReport.activityRecordReport;
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
    public List<GetUserFeedListResDto.FeedDto> findUserFeedList(List<Long> hobbyIds, Long lastRecordId, Integer feedSize, String userId, List<RecordVisibility> visibilities, List<Long> reportedRecordIds, String currentUserId) {
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
                        record.deleted.isFalse(), // 삭제 안된 기록만 조회
                        JPAExpressions
                                .selectFrom(activityRecordReport)
                                .where(
                                        activityRecordReport.reportedRecord.id.eq(record.id),
                                        activityRecordReport.reporter.id.eq(currentUserId)
                                ).notExists()
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
                                record.user.id,
                                record.user.deleted,
                                record.user.nickname,
                                record.visibility,
                                record.deleted
                        ))
                        .from(record)
                        .join(record.user, user)
                        .where(record.id.eq(recordId))
                        .fetchOne()
        );
    }


    @Override
    public List<GetActivityRecordByStoryResDto.RecordDto> getActivityRecordByStory(
            Long hobbyInfoId, Long lastRecordId, Integer size, String keyword,
            String currentUserId, List<String> myFriendIds, List<String> blockFriendIds,
            List<Long> reportedRecordIds, StoryFilterType storyFilterType, String hobbyName) {

        // 1. HOT 필터일 경우 Redis에서 ID 리스트 미리 조회
        List<Long> hotIds = null;
        if (storyFilterType == StoryFilterType.HOT) {
            Double lastScore = (lastRecordId != null) ? redisReactionService.getScore(lastRecordId) : null;
            hotIds = redisReactionService.getHotRecordIdsByCursor(lastScore, lastRecordId, size);
            if (hotIds.isEmpty()) return Collections.emptyList();
        }

        // 2. 메인 쿼리 실행
        return queryFactory
                .select(Projections.constructor(GetActivityRecordByStoryResDto.RecordDto.class,
                        record.id,               // recordId
                        record.imageUrl,         // thumbnailUrl
                        record.sticker,          // sticker
                        activity.content,        // title (활동 내용)
                        record.memo,             // memo
                        Projections.constructor(GetActivityRecordByStoryResDto.UserInfoDto.class, // userInfoDto
                                user.id,
                                user.nickname,
                                user.profileImageUrl
                        ),
                        reaction.id.isNotNull()  // pressedAweSome (좋아요 여부)
                ))
                .from(record)
                .join(record.activity, activity)
                .join(record.user, user)
                .join(record.hobby, hobby)
                // 서브쿼리 대신 Left Join으로 내가 반응했는지 체크 (N+1 방지)
                .leftJoin(reaction).on(
                        reaction.activityRecord.id.eq(record.id),
                        reaction.reactedUser.id.eq(currentUserId)
                )
                .where(
                        hobbyCondition(hobbyInfoId, hobbyName),
                        user.id.ne(currentUserId),
                        user.deleted.isFalse(),
                        record.deleted.isFalse(),
                        storyFilterType == StoryFilterType.HOT ? record.id.in(hotIds) : ltLastRecordId(lastRecordId),
                        notInBlockList(blockFriendIds),
                        notInReportedList(reportedRecordIds),
                        containsKeyword(keyword),
                        getVisibilityCondition(storyFilterType, myFriendIds)
                )
                .orderBy(createOrderSpecifier(storyFilterType, hotIds))
                .limit(size + 1) // hasNext 판단을 위해 1개 더 조회
                .fetch();
    }

// --- Helper Methods ---

    private BooleanExpression hobbyCondition(Long hobbyInfoId, String hobbyName) {
        return record.hobby.hobbyInfoId.eq(hobbyInfoId)
                .or(record.hobby.hobbyName.eq(hobbyName));
    }

    private BooleanExpression ltLastRecordId(Long lastRecordId) {

        return lastRecordId != null ? record.id.lt(lastRecordId) : null;

    }

    private OrderSpecifier<?>[] createOrderSpecifier(StoryFilterType type, List<Long> hotIds) {
        if (type == StoryFilterType.HOT && hotIds != null && !hotIds.isEmpty()) {
            return new OrderSpecifier<?>[]{
                    new OrderSpecifier<>(Order.ASC, Expressions.stringTemplate("FIELD({0}, {1})",
                            record.id, Expressions.constant(hotIds))),
                    record.createdAt.desc()
            };
        }
        return new OrderSpecifier<?>[]{record.id.desc()};
    }

    private BooleanExpression getVisibilityCondition(StoryFilterType type, List<String> myFriendIds) {
        if (type == StoryFilterType.MY_FRIEND) {
            return record.visibility.eq(RecordVisibility.FRIEND)
                    .and(user.id.in(myFriendIds));
        }
        return record.visibility.eq(RecordVisibility.PUBLIC)
                .or(record.visibility.eq(RecordVisibility.FRIEND).and(user.id.in(myFriendIds)));
    }

    private BooleanExpression notInReportedList(List<Long> reportedRecordIds) {
        return (reportedRecordIds == null || reportedRecordIds.isEmpty()) ? null : record.id.notIn(reportedRecordIds);
    }

    private BooleanExpression notInBlockList(List<String> blockFriendIds) {
        return (blockFriendIds == null || blockFriendIds.isEmpty()) ? null : user.id.notIn(blockFriendIds);
    }

    private BooleanExpression containsKeyword(String keyword) {
        return (keyword == null || keyword.isBlank()) ? null : activity.content.contains(keyword).or(record.memo.contains(keyword));
    }


    private BooleanExpression hobbyIdIn(List<Long> hobbyIds) {
        if (hobbyIds == null || hobbyIds.isEmpty()) return null;
        return record.hobby.id.in(hobbyIds);
    }


}
