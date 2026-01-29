package com.example.ForDay.domain.friend.repository;

import com.example.ForDay.domain.friend.dto.response.GetFriendListResDto;
import com.example.ForDay.domain.friend.entity.QFriendRelation;
import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.user.entity.QUser;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class FriendRelationRepositoryImpl implements FriendRelationRepositoryCustom{
    private final JPAQueryFactory queryFactory;
    private QUser user = QUser.user;
    private QFriendRelation relation = QFriendRelation.friendRelation;

    @Override
    public List<GetFriendListResDto.UserInfoDto> findMyFriendList(String currentUserId) {
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
                        user.deleted.isFalse()
                )
                .orderBy(relation.createdAt.desc())
                .fetch();
    }
}
