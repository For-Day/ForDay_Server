package com.example.ForDay.domain.reaction.repository;

import com.example.ForDay.domain.reaction.entity.ActivityRecordReaction;
import com.example.ForDay.domain.record.dto.response.GetRecordReactionUsersResDto;
import com.example.ForDay.domain.record.dto.response.ReactionSummaryResDto;
import com.example.ForDay.domain.record.entity.QActivityRecordReaction;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.QUser;
import com.example.ForDay.infra.s3.util.S3Util;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ActivityRecordReactionRepositoryImpl implements ActivityRecordReactionRepositoryCustom {
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
    public List<GetRecordReactionUsersResDto.ReactionUserInfo> findReactionUsersDtoByType(
            Long recordId, RecordReactionType type, String lastUserId, Integer size, boolean isRecordOwner) {

        return queryFactory
                .select(Projections.constructor(GetRecordReactionUsersResDto.ReactionUserInfo.class,
                        user.id,
                        user.nickname,
                        user.profileImageUrl,
                        activityRecordReaction.createdAt,
                        new CaseBuilder()
                                .when(
                                        Expressions.asBoolean(isRecordOwner).isTrue() // 전달받은 자바 변수가 true이고
                                                .and(activityRecordReaction.readWriter.isFalse()) // DB의 readWriter가 false이면
                                )
                                .then(true)
                                .otherwise(false)
                ))
                .from(activityRecordReaction)
                .join(activityRecordReaction.reactedUser, user)
                .where(
                        activityRecordReaction.activityRecord.id.eq(recordId),
                        activityRecordReaction.reactionType.eq(type),
                        ltLastUserId(lastUserId)
                )
                .orderBy(
                        activityRecordReaction.readWriter.asc(),
                        activityRecordReaction.createdAt.desc()
                )
                .limit(size + 1)
                .fetch();
    }

    @Override
    public Map<RecordReactionType, ReactionSummaryResDto.ReactionSliceDto> getReactionSummary(Long recordId, int size, String currentUserId) {
        NumberExpression<Integer> myReactionPriority = new CaseBuilder()
                .when(activityRecordReaction.reactedUser.id.eq(currentUserId)).then(0)
                .otherwise(1);

        List<ActivityRecordReaction> allReactions = queryFactory
                .selectFrom(activityRecordReaction)
                .join(activityRecordReaction.reactedUser).fetchJoin()
                .where(activityRecordReaction.activityRecord.id.eq(recordId))
                .orderBy(
                        myReactionPriority.asc(),
                        activityRecordReaction.createdAt.desc())
                .fetch();

        return Arrays.stream(RecordReactionType.values())
                .collect(Collectors.toMap(
                        type -> type,
                        type -> {
                            List<ReactionSummaryResDto.ReactionUserDto> filteredUsers = allReactions.stream()
                                    .filter(r -> r.getReactionType() == type)
                                    .limit(size + 1) // 다음 페이지 확인용
                                    .map(r -> ReactionSummaryResDto.ReactionUserDto.builder()
                                            .userId(r.getReactedUser().getId())
                                            .nickname(r.getReactedUser().getNickname())
                                            .profileImageUrl(r.getReactedUser().getProfileImageUrl())
                                            .reactionType(r.getReactionType())
                                            .build())
                                    .collect(Collectors.toList());

                            boolean hasNext = filteredUsers.size() > size;
                            if (hasNext) {
                                filteredUsers.remove(size);
                            }

                            return ReactionSummaryResDto.ReactionSliceDto.builder()
                                    .users(filteredUsers)
                                    .hasNext(hasNext)
                                    .nextCursor(null)
                                    .build();
                        }
                ));
    }

    private BooleanExpression ltLastUserId(String lastUserId) {
        if (lastUserId == null) {
            return null;
        }
        return activityRecordReaction.reactedUser.id.gt(lastUserId);
    }
}
