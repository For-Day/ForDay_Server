package com.example.ForDay.domain.activity.repository;

import com.example.ForDay.domain.activity.entity.ActivityRecommendItem;
import com.example.ForDay.domain.activity.entity.QActivityRecommendItem;
import com.example.ForDay.domain.activity.type.AIItemType;
import com.example.ForDay.domain.hobby.entity.QHobby;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
public class ActivityRecommendItemRepositoryImpl implements ActivityRecommendItemRepositoryCustom{
    private final JPAQueryFactory queryFactory;

    QActivityRecommendItem activityRecommendItem = QActivityRecommendItem.activityRecommendItem;
    QHobby hobby = QHobby.hobby;

    @Override
    public List<ActivityRecommendItem> findAllByHobbyIdAndDate(Long hobbyId, LocalDateTime startOfToday, LocalDateTime endOfToday, AIItemType type) {
        JPAQuery<ActivityRecommendItem> query = queryFactory
                .selectFrom(activityRecommendItem)
                .where(
                        hobby.id.eq(hobbyId),
                        activityRecommendItem.createdAt.between(startOfToday, endOfToday)
                )
                .orderBy(activityRecommendItem.createdAt.desc(),
                        activityRecommendItem.id.desc());

        if (type == AIItemType.LATEST) {
            query.limit(3);
        }

        return query.fetch();
    }
}
