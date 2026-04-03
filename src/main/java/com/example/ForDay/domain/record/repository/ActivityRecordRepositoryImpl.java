package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.activity.entity.QActivity;
import com.example.ForDay.domain.friend.entity.QFriendRelation;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.hobby.dto.response.GetStickerInfoResDto;
import com.example.ForDay.domain.hobby.entity.QHobby;
import com.example.ForDay.domain.reaction.entity.QActivityRecordReaction;
import com.example.ForDay.domain.record.dto.ActivityRecordWithUserDto;
import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.dto.ReportActivityRecordDto;
import com.example.ForDay.domain.record.dto.request.RecordSearchConditionReqDto;
import com.example.ForDay.domain.record.dto.response.GetActivityRecordByStoryResDto;
import com.example.ForDay.domain.record.entity.QActivityRecord;
import com.example.ForDay.domain.record.entity.QActivityRecordReport;
import com.example.ForDay.domain.record.entity.QActivityRecordScrap;
import com.example.ForDay.domain.reaction.service.ReactionRankingService;
import com.example.ForDay.domain.record.type.ContextType;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.record.type.StoryFilterType;
import com.example.ForDay.domain.user.dto.response.GetUserFeedListResDto;
import com.example.ForDay.domain.user.entity.QUser;
import com.example.ForDay.domain.user.type.Role;
import com.querydsl.core.BooleanBuilder;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ActivityRecordRepositoryImpl implements ActivityRecordRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final ReactionRankingService reactionRankingService;

    private final QActivityRecord record = QActivityRecord.activityRecord;
    private final QUser user = QUser.user;
    private final QActivity activity = QActivity.activity;
    private final QActivityRecordReaction reaction = QActivityRecordReaction.activityRecordReaction;
    private final QActivityRecordReport activityRecordReport = QActivityRecordReport.activityRecordReport;
    private final QHobby hobby = QHobby.hobby;
    private final QActivityRecordScrap scrap = QActivityRecordScrap.activityRecordScrap;

    @Override
    public List<GetStickerInfoResDto.StickerDto> getStickerInfo(
            Long hobbyId,
            Integer currentPage,
            Integer size,
            String currentUserId
    ) {
        QActivityRecord record = QActivityRecord.activityRecord;

        int safePage = (currentPage == null || currentPage <= 0) ? 1 : currentPage;
        int safeSize = (size == null || size <= 0) ? 10 : size;

        int offset = (safePage - 1) * safeSize;

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
                        record.user.id.eq(currentUserId)
                )
                .orderBy(record.createdAt.asc())
                .offset(offset)
                .limit(safeSize)
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
                .join(record.user, user)
                .where(
                        user.id.eq(userId),
                        ltLastRecordId(lastRecordId),
                        hobbyIdIn(hobbyIds),
                        record.visibility.in(visibilities),
                        record.deleted.isFalse(),
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
                                record.deleted,
                                record.imageUrl
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
            Double lastScore = (lastRecordId != null) ? reactionRankingService.getScore(lastRecordId) : null;
            hotIds = reactionRankingService.getHotRecordIdsByCursor(lastScore, lastRecordId, size);
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
                                        (blockRelation.requester.id.eq(currentUserId).and(blockRelation.targetUser.id.eq(record.user.id))
                                                .or(blockRelation.requester.id.eq(record.user.id).and(blockRelation.targetUser.id.eq(currentUserId))))
                                                .and(blockRelation.relationStatus.eq(FriendRelationStatus.BLOCK))
                                                .or(
                                                        blockRelation.requester.id.eq(currentUserId)
                                                                .and(blockRelation.targetUser.id.eq(record.user.id))
                                                                .and(blockRelation.relationStatus.eq(FriendRelationStatus.REPORT))
                                                )
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

    @Override
    public Long findPrevRecordId(Long currentId, LocalDateTime currentCreatedAt, RecordSearchConditionReqDto cond, String currentUserId, List<Long> hobbyIds) {
        Long currentScrapId = getScrapIdIfContextIsScrap(currentId, cond, currentUserId);

        return queryFactory
                .select(record.id)
                .from(record)
                .leftJoin(scrap).on(scrap.activityRecord.id.eq(record.id))
                .where(
                        cond.context() == ContextType.USER_SCRAP
                                ? (currentScrapId != null ? scrap.id.gt(currentScrapId) : null) // 더 최근에 스크랩한 것
                                : (record.createdAt.gt(currentCreatedAt)
                                .or(record.createdAt.eq(currentCreatedAt).and(record.id.gt(currentId)))),
                        commonFilter(cond, currentUserId, hobbyIds)
                )
                .orderBy(cond.context() == ContextType.USER_SCRAP
                        ? scrap.id.asc()
                        : record.createdAt.asc(), record.id.asc())
                .limit(1)
                .fetchOne();
    }

    @Override
    public Long findNextRecordId(Long currentId, LocalDateTime currentCreatedAt, RecordSearchConditionReqDto cond, String currentUserId, List<Long> hobbyIds) {
        // 스크랩 컨텍스트일 경우 현재 글의 scrapId를 조회
        Long currentScrapId = getScrapIdIfContextIsScrap(currentId, cond, currentUserId);

        return queryFactory
                .select(record.id)
                .from(record)
                .leftJoin(scrap).on(scrap.activityRecord.id.eq(record.id))
                .where(
                        cond.context() == ContextType.USER_SCRAP
                                ? (currentScrapId != null ? scrap.id.lt(currentScrapId) : null) // 더 이전에 스크랩한 것
                                : (record.createdAt.lt(currentCreatedAt)
                                .or(record.createdAt.eq(currentCreatedAt).and(record.id.lt(currentId)))),
                        commonFilter(cond, currentUserId, hobbyIds)
                )
                .orderBy(cond.context() == ContextType.USER_SCRAP
                        ? scrap.id.desc()
                        : record.createdAt.desc(), record.id.desc())
                .limit(1)
                .fetchOne();
    }

    private Long getScrapIdIfContextIsScrap(Long currentId, RecordSearchConditionReqDto cond, String currentUserId) {
        if (cond.context() != ContextType.USER_SCRAP) return null;

        String targetId = (!StringUtils.hasText(cond.userId())) ? currentUserId : cond.userId();
        return queryFactory
                .select(scrap.id)
                .from(scrap)
                .where(
                        scrap.activityRecord.id.eq(currentId),
                        scrap.user.id.eq(targetId)
                )
                .fetchOne();
    }

    private BooleanBuilder commonFilter(RecordSearchConditionReqDto cond, String currentUserId, List<Long> hobbyIds) {
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(record.deleted.isFalse().and(record.user.deleted.isFalse()));
        builder.and(record.user.deleted.isFalse());
        builder.and(notReportedBy(currentUserId));
        builder.and(notBlockedOrReported(currentUserId));

        if (cond.context() == ContextType.STORY_ALL || cond.context() == ContextType.STORY_HOBBY) {
            builder = builder.and(record.user.role.eq(Role.USER));
        }

        String targetId = (!StringUtils.hasText(cond.userId())) ? currentUserId : cond.userId();

        switch (cond.context()) {
            case STORY_ALL -> {
                if(StringUtils.hasText(cond.keyword())) {
                    builder.and(activity.content.contains(cond.keyword()).or(record.memo.contains(cond.keyword())));
                }
                builder.and(publicOrFriendVisibility(currentUserId));
            }
            case STORY_HOBBY -> {
                // hobbyIds 리스트의 첫 번째 값을 기준으로 이름/InfoId 필터링
                if (hobbyIds != null && !hobbyIds.isEmpty()) {
                    Long baseHobbyId = hobbyIds.get(0);
                    QHobby subHobby = new QHobby("subHobby");

                    builder.and(
                            record.hobby.hobbyName.eq(
                                            JPAExpressions.select(subHobby.hobbyName)
                                                    .from(subHobby)
                                                    .where(subHobby.id.eq(baseHobbyId))
                                    )
                                    .or(
                                            record.hobby.hobbyInfoId.isNotNull()
                                                    .and(record.hobby.hobbyInfoId.eq(
                                                            JPAExpressions.select(subHobby.hobbyInfoId)
                                                                    .from(subHobby)
                                                                    .where(subHobby.id.eq(baseHobbyId))
                                                    ))
                                    )
                    );
                }
                if(StringUtils.hasText(cond.keyword())) {
                    builder.and(activity.content.contains(cond.keyword()).or(record.memo.contains(cond.keyword())));
                }
                builder.and(publicOrFriendVisibility(currentUserId));
            }
            case USER_FEED -> {
                builder.and(record.user.id.eq(targetId));
                if (!targetId.equals(currentUserId)) {
                    builder.and(publicOrFriendVisibility(currentUserId));
                }
                // 기존 직접 in 필터 대신 공통 메서드 사용 (비어있으면 자동 null 처리로 전체조회)
                builder.and(hobbyIdIn(hobbyIds));
            }
            case USER_SCRAP -> {
                builder.and(scrap.user.id.eq(targetId));
                builder.and(record.user.id.eq(currentUserId)
                        .or(publicOrFriendVisibility(currentUserId)));
            }
        }
        return builder;
    }

    private BooleanExpression publicOrFriendVisibility(String currentUserId) {
        return record.visibility.eq(RecordVisibility.PUBLIC)
                .or(record.visibility.eq(RecordVisibility.FRIEND)
                        .and(isFollowing(currentUserId, record.user.id)));
    }

    private BooleanExpression hobbyCondition(Long hobbyInfoId, String hobbyName) {
        if (hobbyInfoId == null && !StringUtils.hasText(hobbyName)) {
            return null;
        }
        if (hobbyInfoId != null && StringUtils.hasText(hobbyName)) {
            return record.hobby.hobbyInfoId.eq(hobbyInfoId)
                    .or(record.hobby.hobbyName.eq(hobbyName));
        }
        if (hobbyInfoId != null) {
            return record.hobby.hobbyInfoId.eq(hobbyInfoId);
        }

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

    private BooleanExpression notReportedBy(String currentUserId) {
        if (currentUserId == null) return null;
        QActivityRecordReport report = QActivityRecordReport.activityRecordReport;
        return JPAExpressions
                .selectFrom(report)
                .where(
                        report.reportedRecord.id.eq(record.id),
                        report.reporter.id.eq(currentUserId)
                ).notExists();
    }

    // 내가 상대방을 팔로우 중인지 확인 (FRIEND 권한 확인용)
    private BooleanExpression isFollowing(String requesterId, StringPath targetUserIdPath) {
        if (requesterId == null) return Expressions.asBoolean(false).isTrue().and(Expressions.asBoolean(false));
        QFriendRelation followRelation = new QFriendRelation("followRelation");
        return JPAExpressions
                .selectFrom(followRelation)
                .where(
                        followRelation.requester.id.eq(requesterId),
                        followRelation.targetUser.id.eq(targetUserIdPath),
                        followRelation.relationStatus.eq(FriendRelationStatus.FOLLOW)
                ).exists();
    }

    // 상호 차단 및 내가 신고한 유저 제외
    private BooleanExpression notBlockedOrReported(String currentUserId) {
        if (currentUserId == null) return null;
        QFriendRelation rel = new QFriendRelation("rel");

        return JPAExpressions
                .selectFrom(rel)
                .where(
                        // 1. 상호 차단 (A가 B를 BLOCK 하거나 OR B가 A를 BLOCK 한 경우)
                        (
                                (rel.requester.id.eq(currentUserId).and(rel.targetUser.id.eq(record.user.id)))
                                        .or(rel.requester.id.eq(record.user.id).and(rel.targetUser.id.eq(currentUserId)))
                        ).and(rel.relationStatus.eq(FriendRelationStatus.BLOCK))

                                .or(
                                        // 2. 단방향 신고 (내가 게시글 작성자를 REPORT 한 경우만)
                                        rel.requester.id.eq(currentUserId)
                                                .and(rel.targetUser.id.eq(record.user.id))
                                                .and(rel.relationStatus.eq(FriendRelationStatus.REPORT))
                                )
                ).notExists();
    }

}

