package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.activity.entity.QActivity;
import com.example.ForDay.domain.friend.entity.QFriendRelation;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
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
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.record.type.StoryFilterType;
import com.example.ForDay.domain.user.dto.response.GetUserFeedListResDto;
import com.example.ForDay.domain.user.entity.QUser;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.domain.user.type.Role;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringPath;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

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
    public List<GetUserFeedListResDto.FeedDto> findUserFeedList(List<Long> hobbyIds, Long lastRecordId, Integer feedSize, String userId, List<RecordVisibility> visibilities, String currentUserId) {
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
                .orderBy(record.createdAt.desc())
                .limit(feedSize + 1)
                .fetch();
    }

    @Override
    public Optional<RecordDetailQueryDto> findDetailDtoById(Long recordId) {
        return Optional.ofNullable(queryFactory
                .select(Projections.constructor(RecordDetailQueryDto.class,
                        record.hobby.id,
                        record.hobby.hobbyName,
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
            String currentUserId, StoryFilterType storyFilterType, String hobbyName) {

        // HOT 필터일 경우 Redis 조회
        List<Long> hotIds = null;
        if (storyFilterType == StoryFilterType.HOT) {
            Double lastScore = (lastRecordId != null) ? redisReactionService.getScore(lastRecordId) : null;
            hotIds = redisReactionService.getHotRecordIdsByCursor(lastScore, lastRecordId, size);
            if (hotIds.isEmpty()) return Collections.emptyList();
        }

        QFriendRelation blockRelation = new QFriendRelation("blockRelation");
        QFriendRelation followRelation = new QFriendRelation("followRelation");
        QActivityRecordReport report = QActivityRecordReport.activityRecordReport;

        return queryFactory
                .select(Projections.constructor(GetActivityRecordByStoryResDto.RecordDto.class,
                        record.id,
                        record.imageUrl,
                        record.sticker,
                        activity.content,
                        record.memo,
                        Projections.constructor(GetActivityRecordByStoryResDto.UserInfoDto.class,
                                user.id, user.nickname, user.profileImageUrl
                        ),
                        reaction.id.isNotNull(),
                        hobby.hobbyName,
                        record.user.id.eq(currentUserId)
                ))
                .from(record)
                .join(record.activity, activity)
                .join(record.user, user)
                .join(record.hobby, hobby)
                .leftJoin(reaction).on(
                        reaction.activityRecord.id.eq(record.id),
                        reaction.reactedUser.id.eq(currentUserId),
                        reaction.reactionType.eq(RecordReactionType.GREAT)
                )
                .where(
                        hobbyCondition(hobbyInfoId, hobbyName),
                        user.deleted.isFalse(),
                        user.role.eq(Role.USER),
                        record.deleted.isFalse(),
                        storyFilterType == StoryFilterType.HOT ? record.id.in(hotIds) : ltLastRecordId(lastRecordId),
                        containsKeyword(keyword),
                        // 상호 차단 관계 배제 (내가 차단했거나, 상대가 나를 차단했거나)
                        JPAExpressions
                                .selectFrom(blockRelation)
                                .where(
                                        (blockRelation.requester.id.eq(currentUserId).and(blockRelation.targetUser.id.eq(user.id)))
                                                .or(blockRelation.requester.id.eq(user.id).and(blockRelation.targetUser.id.eq(currentUserId))),
                                        blockRelation.relationStatus.eq(FriendRelationStatus.BLOCK)
                                ).notExists(),
                        // 내가 신고한 게시글 배제
                        JPAExpressions
                                .selectFrom(report)
                                .where(
                                        report.reportedRecord.id.eq(record.id),
                                        report.reporter.id.eq(currentUserId)
                                ).notExists(),
                        // 공개 범위(Visibility) 필터링
                        record.visibility.eq(RecordVisibility.PUBLIC) // PUBLIC은 기본 통과
                                .or(record.visibility.eq(RecordVisibility.FRIEND) // FRIEND인 경우
                                        .and(JPAExpressions // 내가 작성자를 팔로우 중인지 확인
                                                .selectFrom(followRelation)
                                                .where(
                                                        followRelation.requester.id.eq(currentUserId),
                                                        followRelation.targetUser.id.eq(user.id),
                                                        followRelation.relationStatus.eq(FriendRelationStatus.FOLLOW)
                                                ).exists()
                                        )
                                        .or(record.user.id.eq(currentUserId))
                                )
                )
                .orderBy(createOrderSpecifier(storyFilterType, hotIds))
                .limit(size + 1)
                .fetch();
    }

    private BooleanExpression hobbyCondition(Long hobbyInfoId, String hobbyName) {
        if (hobbyInfoId == null && !StringUtils.hasText(hobbyName)) {
            return null;
        }

        // 2. 둘 다 값이 있는 경우 (OR 조건)
        if (hobbyInfoId != null && StringUtils.hasText(hobbyName)) {
            return record.hobby.hobbyInfoId.eq(hobbyInfoId)
                    .or(record.hobby.hobbyName.eq(hobbyName));
        }

        // 3. hobbyInfoId만 있는 경우
        if (hobbyInfoId != null) {
            return record.hobby.hobbyInfoId.eq(hobbyInfoId);
        }

        // 4. hobbyName만 있는 경우
        return record.hobby.hobbyName.eq(hobbyName);
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

    private BooleanExpression containsKeyword(String keyword) {
        return (keyword == null || keyword.isBlank()) ? null : activity.content.contains(keyword).or(record.memo.contains(keyword));
    }

    private BooleanExpression hobbyIdIn(List<Long> hobbyIds) {
        if (hobbyIds == null || hobbyIds.isEmpty()) return null;
        return record.hobby.id.in(hobbyIds);
    }


}
