package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.ActivityRecordReaction;
import com.example.ForDay.domain.record.entity.QActivityRecordReaction;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.QUser;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ActivityRecordReactionRepositoryImpl implements ActivityRecordReactionRepositoryCustom{
    private final JPAQueryFactory queryFactory;
    private QActivityRecordReaction activityRecordReaction = QActivityRecordReaction.activityRecordReaction;
    private QUser user = QUser.user;

    @Override
    public List<RecordReactionType> findAllMyReactions(Long activityRecordId, String currentUserId) {
        return queryFactory
                .select(activityRecordReaction.reactionType)
                .from(activityRecordReaction)
                .where(activityRecordReaction.activityRecord.id.eq(activityRecordId),
                        activityRecordReaction.reactedUser.id.eq(currentUserId))
                .fetch();
    }

    @Override
    public List<RecordReactionType> findAllUnreadReactions(Long activityRecordId) {
        return queryFactory
                .select(activityRecordReaction.reactionType)
                .from(activityRecordReaction)
                .where(activityRecordReaction.activityRecord.id.eq(activityRecordId),
                        activityRecordReaction.readWriter.eq(false))
                .fetch();
    }

    @Override
    public List<ActivityRecordReaction> findUnreadReactionsByType(Long recordId, RecordReactionType type) {
        return queryFactory
                .selectFrom(activityRecordReaction)
                .join(activityRecordReaction.reactedUser, user).fetchJoin() // 유저 정보 페치 조인
                .where(
                        activityRecordReaction.activityRecord.id.eq(recordId),
                        activityRecordReaction.reactionType.eq(type),
                        activityRecordReaction.readWriter.eq(false)
                )
                .orderBy(activityRecordReaction.createdAt.desc()) // 최신순 정렬
                .fetch();
    }
}
