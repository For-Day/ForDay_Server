package com.example.ForDay.domain.friend.repository;

import com.example.ForDay.domain.friend.dto.response.GetFriendListResDto;
import com.example.ForDay.domain.friend.entity.QFriendRelation;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.user.entity.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RequiredArgsConstructor
public class FriendRelationRepositoryImpl implements FriendRelationRepositoryCustom{
    private final JPAQueryFactory queryFactory;
    private QUser user = QUser.user;
    private QFriendRelation relation = QFriendRelation.friendRelation;

    @Override
    public List<GetFriendListResDto.UserInfoDto> findMyFriendList(String currentUserId, String lastUserId, Integer size) {
        // 나를 차단한 유저들의 ID를 찾는 서브쿼리
        JPQLQuery<String> blockedMeUserIds = JPAExpressions
                .select(relation.requester.id)
                .from(relation)
                .where(
                        relation.targetUser.id.eq(currentUserId),
                        relation.relationStatus.eq(FriendRelationStatus.BLOCK)
                );

        return queryFactory
                .select(Projections.constructor(GetFriendListResDto.UserInfoDto.class,
                        user.id,
                        user.nickname,
                        user.profileImageUrl
                ))
                .from(relation)
                .join(relation.targetUser, user)
                .where(
                        relation.requester.id.eq(currentUserId),
                        relation.relationStatus.eq(FriendRelationStatus.FOLLOW),
                        user.deleted.isFalse(),
                        user.id.notIn(blockedMeUserIds),
                        ltLastUserId(lastUserId)
                )
                .orderBy(relation.createdAt.desc(), user.id.desc())
                .limit(size + 1)
                .fetch();
    }

    @Override
    public List<String> findAllBlockedIdsByUserId(String userId) {
        QFriendRelation friendRelation = QFriendRelation.friendRelation;

        // 1. 내가 차단한 사람들
        List<String> blockedByMe = queryFactory
                .select(friendRelation.targetUser.id)
                .from(friendRelation)
                .where(friendRelation.requester.id.eq(userId)
                        .and(friendRelation.relationStatus.eq(FriendRelationStatus.BLOCK)))
                .fetch();

        // 2. 나를 차단한 사람들
        List<String> blockedMe = queryFactory
                .select(friendRelation.requester.id)
                .from(friendRelation)
                .where(friendRelation.targetUser.id.eq(userId)
                        .and(friendRelation.relationStatus.eq(FriendRelationStatus.BLOCK)))
                .fetch();

        // 두 리스트 합치기 (중복 제거를 위해 Set 활용 가능)
        Set<String> allBlockedIds = new HashSet<>(blockedByMe);
        allBlockedIds.addAll(blockedMe);

        return new ArrayList<>(allBlockedIds);
    }

    private BooleanExpression ltLastUserId(String lastUserId) {
        if (lastUserId == null || lastUserId.isEmpty()) {
            return null;
        }
        return user.id.lt(lastUserId);
    }
}
