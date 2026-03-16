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
public class FriendRelationRepositoryImpl implements FriendRelationRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private QUser user = QUser.user;
    private QFriendRelation relation = QFriendRelation.friendRelation;

    @Override
    public List<GetFriendListResDto.UserInfoDto> findMyFriendList(String currentUserId, String lastUserId, Integer size) {
        QFriendRelation subRelation = new QFriendRelation("subRelation");

        return queryFactory
                .select(Projections.constructor(GetFriendListResDto.UserInfoDto.class,
                        user.id,
                        user.nickname,
                        user.profileImageUrl
                ))
                .from(relation)
                .join(relation.targetUser, user)
                .leftJoin(subRelation).on(
                        subRelation.requester.id.eq(user.id),
                        subRelation.targetUser.id.eq(currentUserId),
                        subRelation.relationStatus.eq(FriendRelationStatus.BLOCK)
                )
                .where(
                        relation.requester.id.eq(currentUserId),
                        relation.relationStatus.eq(FriendRelationStatus.FOLLOW),
                        user.deleted.isFalse(),
                        subRelation.id.isNull(),
                        ltLastUserId(lastUserId)
                )
                .orderBy(relation.createdAt.desc(), user.id.desc())
                .limit(size + 1)
                .fetch();
    }

    @Override
    public List<String> findAllBlockedIdsByUserId(String userId) {
        QFriendRelation friendRelation = QFriendRelation.friendRelation;

        // 1. 내가 차단하거나 신고한 사람들
        List<String> blockedByMe = queryFactory
                .select(friendRelation.targetUser.id)
                .from(friendRelation)
                .where(
                        friendRelation.requester.id.eq(userId),
                        friendRelation.relationStatus.in(FriendRelationStatus.BLOCK, FriendRelationStatus.REPORT) // 이 부분이 괄호로 묶인 OR 역할
                )
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
