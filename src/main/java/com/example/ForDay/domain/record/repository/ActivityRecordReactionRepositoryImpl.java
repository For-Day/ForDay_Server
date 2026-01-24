package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.ActivityRecordReaction;
import com.example.ForDay.domain.record.entity.QActivityRecordReaction;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.QUser;
import com.querydsl.core.types.dsl.BooleanExpression;
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
    public List<ActivityRecordReaction> findUsersReactionsByType(Long recordId, RecordReactionType type, String lastUserId, Integer size) {
        return queryFactory
                .selectFrom(activityRecordReaction)
                .join(activityRecordReaction.reactedUser, user).fetchJoin()
                .where(
                        activityRecordReaction.activityRecord.id.eq(recordId),
                        activityRecordReaction.reactionType.eq(type),
                        ltLastUserId(lastUserId) // 커서 조건 추가
                )
                .orderBy(
                        activityRecordReaction.readWriter.asc(),
                        activityRecordReaction.createdAt.desc()
                )
                .limit(size + 1)
                .fetch();
    }

    // lastUserId가 있을 때만 '다음 페이지' 조건 생성
    private BooleanExpression ltLastUserId(String lastUserId) {
        if (lastUserId == null) {
            return null;
        }
        // 실제 운영 환경에서는 ID가 아닌 정렬 기준값(ex: createdAt)으로 비교하는 것이 더 빠릅니다.
        // 여기서는 단순성을 위해 ID 기반 예시를 들거나,
        // 정렬 순서상 특정 시점 이후의 데이터를 가져오는 로직을 넣어야 합니다.
        return activityRecordReaction.reactedUser.id.gt(lastUserId);
    }
}
