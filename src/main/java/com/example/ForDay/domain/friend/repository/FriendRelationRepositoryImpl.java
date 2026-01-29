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

import java.util.List;

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

    private BooleanExpression ltLastUserId(String lastUserId) {
        if (lastUserId == null || lastUserId.isEmpty()) {
            return null;
        }
        return user.id.lt(lastUserId);
    }
}
