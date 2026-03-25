package com.example.ForDay.domain.reaction.repository;

import com.example.ForDay.domain.reaction.entity.ActivityRecordReaction;
import com.example.ForDay.domain.reaction.entity.QActivityRecordReaction;
import com.example.ForDay.domain.record.dto.response.GetRecordReactionUsersResDto;
import com.example.ForDay.domain.record.dto.response.ReactionSummaryResDto;
import com.example.ForDay.domain.record.dto.response.ReactionTabScrollResDto;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

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

        Map<String, ReactionSummaryResDto.ReactionSliceDto> resultTabs = new LinkedHashMap<>();

        // 기록에 반응한 전체 사용자 조회
        List<ActivityRecordReaction> allReactions = fetchReactionsByType(recordId, null, size, currentUserId);
        resultTabs.put("ALL", createSliceDto(allReactions, size));

        // 각 반응 타입별 탭 조회 (각각 독립 쿼리 수행)
        for (RecordReactionType type : RecordReactionType.values()) {
            List<ActivityRecordReaction> typeReactions = fetchReactionsByType(recordId, type, size, currentUserId);
            resultTabs.put(type.name(), createSliceDto(typeReactions, size));
        }

        return resultTabs;
    }

    @Override
    public ReactionTabScrollResDto getReactionTabScroll(
            Long recordId, RecordReactionType type, Long lastReactionId, int size, String currentUserId) {

        List<ActivityRecordReaction> reactions = queryFactory
                .selectFrom(activityRecordReaction)
                .join(activityRecordReaction.reactedUser, user).fetchJoin()
                .where(
                        activityRecordReaction.activityRecord.id.eq(recordId),
                        type != null ? activityRecordReaction.reactionType.eq(type) : null,
                        ltLastReactionId(lastReactionId)
                )
                .orderBy(activityRecordReaction.id.desc())
                .limit(size + 1)
                .fetch();

        List<ReactionSummaryResDto.ReactionUserDto> userDtos = reactions.stream()
                .map(r -> ReactionSummaryResDto.ReactionUserDto.builder()
                        .reactionId(r.getId())
                        .userId(r.getReactedUser().getId())
                        .nickname(r.getReactedUser().getNickname())
                        .profileImageUrl(r.getReactedUser().getProfileImageUrl())
                        .reactionType(r.getReactionType())
                        .build())
                .collect(Collectors.toList());

        boolean hasNext = userDtos.size() > size;
        if (hasNext) {
            userDtos.remove(size);
        }

        String tabKey = (type == null) ? "ALL" : type.name();

        ReactionTabScrollResDto.ReactionSliceDto slice = ReactionTabScrollResDto.ReactionSliceDto.builder()
                .users(userDtos)
                .lastReactionId(userDtos.isEmpty() ? null : userDtos.get(userDtos.size() - 1).getReactionId())
                .hasNext(hasNext)
                .build();

        return ReactionTabScrollResDto.builder()
                .tabs(Map.of(tabKey, slice))
                .build();
    }


    private List<ActivityRecordReaction> fetchReactionsByType(Long recordId, RecordReactionType type, int size, String currentUserId) {
        NumberExpression<Integer> myReactionPriority = new CaseBuilder()
                .when(activityRecordReaction.reactedUser.id.eq(currentUserId)).then(0) // 자신의 반응은 최상단으로
                .otherwise(1);

        return queryFactory
                .selectFrom(activityRecordReaction)
                .join(activityRecordReaction.reactedUser, user).fetchJoin()
                .where(
                        activityRecordReaction.activityRecord.id.eq(recordId),
                        type != null ? activityRecordReaction.reactionType.eq(type) : null
                )
                .orderBy(
                        myReactionPriority.asc(),
                        activityRecordReaction.createdAt.desc()
                )
                .limit(size + 1)
                .fetch();
    }

    private ReactionSummaryResDto.ReactionSliceDto createSliceDto(List<ActivityRecordReaction> reactions, int size) {
        // 다음 페이지 여부 확인을 위해 size + 1까지 stream 처리
        List<ReactionSummaryResDto.ReactionUserDto> userDtos = reactions.stream()
                .limit(size + 1)
                .map(r -> ReactionSummaryResDto.ReactionUserDto.builder()
                        .reactionId(r.getId())
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
                .lastReactionId(userDtos.isEmpty() ? null : userDtos.get(userDtos.size() - 1).getReactionId())
                .build();
    }

    private BooleanExpression ltLastUserId(String lastUserId) {
        if (lastUserId == null) {
            return null;
        }
        return activityRecordReaction.reactedUser.id.gt(lastUserId);
    }

    private BooleanExpression ltLastReactionId(Long lastReactionId) {
        if (lastReactionId == null) {
            return null;
        }
        return activityRecordReaction.id.lt(lastReactionId);
    }
}
