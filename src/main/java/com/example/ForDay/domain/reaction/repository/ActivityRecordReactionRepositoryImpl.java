package com.example.ForDay.domain.reaction.repository;

import com.example.ForDay.domain.reaction.entity.ActivityRecordReaction;
import com.example.ForDay.domain.reaction.entity.QActivityRecordReaction;
import com.example.ForDay.domain.record.dto.response.GetRecordReactionUsersResDto;
import com.example.ForDay.domain.record.dto.response.ReactionSummaryResDto;
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
import java.util.LinkedHashMap;
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
    public Map<String, ReactionSummaryResDto.ReactionSliceDto> getReactionSummary(Long recordId, int size, String currentUserId) {
        // 내 반응 우선순위 부여
        NumberExpression<Integer> myReactionPriority = new CaseBuilder()
                .when(activityRecordReaction.reactedUser.id.eq(currentUserId)).then(0)
                .otherwise(1);

        // 전체 반응 조회
        List<ActivityRecordReaction> allReactions = queryFactory
                .selectFrom(activityRecordReaction)
                .join(activityRecordReaction.reactedUser).fetchJoin()
                .where(activityRecordReaction.activityRecord.id.eq(recordId))
                .orderBy(
                        myReactionPriority.asc(),
                        activityRecordReaction.createdAt.desc())
                .fetch();

        Map<String, ReactionSummaryResDto.ReactionSliceDto> resultTabs = new LinkedHashMap<>();
        resultTabs.put("ALL", createSliceDto(allReactions, size));
        for (RecordReactionType type : RecordReactionType.values()) {
            List<ActivityRecordReaction> filtered = allReactions.stream()
                    .filter(r -> r.getReactionType() == type)
                    .toList();

            resultTabs.put(type.name(), createSliceDto(filtered, size));
        }

        return resultTabs;
    }

    private ReactionSummaryResDto.ReactionSliceDto createSliceDto(List<ActivityRecordReaction> reactions, int size) {
        // 다음 페이지 여부 확인을 위해 size + 1까지 stream 처리
        List<ReactionSummaryResDto.ReactionUserDto> userDtos = reactions.stream()
                .limit(size + 1)
                .map(r -> ReactionSummaryResDto.ReactionUserDto.builder()
                        .userId(r.getReactedUser().getId())
                        .nickname(r.getReactedUser().getNickname())
                        .profileImageUrl(r.getReactedUser().getProfileImageUrl())
                        .reactionType(r.getReactionType())
                        .build())
                .collect(Collectors.toList());

        boolean hasNext = userDtos.size() > size;

        // 다음 페이지 데이터가 있다면 결과 리스트에서 제거
        if (hasNext) {
            userDtos.remove(size);
        }

        return ReactionSummaryResDto.ReactionSliceDto.builder()
                .users(userDtos)
                .hasNext(hasNext)
                .lastUserId(null) // 필요시 마지막 유저 ID 등을 할당
                .build();
    }

    private BooleanExpression ltLastUserId(String lastUserId) {
        if (lastUserId == null) {
            return null;
        }
        return activityRecordReaction.reactedUser.id.gt(lastUserId);
    }
}
