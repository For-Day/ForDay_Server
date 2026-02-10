package com.example.ForDay.domain.hobby.repository;

import com.example.ForDay.domain.hobby.entity.QHobbyCard;
import com.example.ForDay.domain.user.dto.response.GetUserHobbyCardListResDto;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class HobbyCardRepositoryImpl implements HobbyCardRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    QHobbyCard hobbyCard = QHobbyCard.hobbyCard;

    @Override
    public List<GetUserHobbyCardListResDto.HobbyCardDto> findUserHobbyCardList(Long lastHobbyCardId, Integer size, String currentUserId) {
        return queryFactory
                .select(Projections.constructor(
                        GetUserHobbyCardListResDto.HobbyCardDto.class,
                        hobbyCard.id,
                        hobbyCard.content,
                        hobbyCard.imageUrl,
                        hobbyCard.createdAt
                ))
                .from(hobbyCard)
                .where(
                        hobbyCard.user.id.eq(currentUserId),
                        ltLastHobbyCardId(lastHobbyCardId)
                )
                .orderBy(hobbyCard.id.desc())
                .limit(size + 1)
                .fetch();
    }

    private BooleanExpression ltLastHobbyCardId(Long lastHobbyCardId) {
        return lastHobbyCardId != null ? hobbyCard.id.lt(lastHobbyCardId) : null;
    }
}
