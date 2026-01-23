package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.QActivityRecordReaction;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ActivityRecordReactionRepositoryImpl implements ActivityRecordReactionRepositoryCustom{
    private final JPAQueryFactory queryFactory;
    private QActivityRecordReaction activityRecordReaction = QActivityRecordReaction.activityRecordReaction;

    @Override
    public List<RecordReactionType> findAllMyReactions(Long activityRecordId, String currentUserId) {
        return queryFactory
                .select(activityRecordReaction.reactionType)
                .from(activityRecordReaction)
                .where(activityRecordReaction.activityRecord.id.eq(activityRecordId),
                        activityRecordReaction.reactedUserId.id.eq(currentUserId))
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
}
