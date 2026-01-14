package com.example.ForDay.domain.activity.repository;

import com.example.ForDay.domain.activity.entity.QActivity;
import com.example.ForDay.domain.hobby.dto.response.GetHobbyActivitiesResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ActivityRepositoryImpl implements ActivityRepositoryCustom{
    private final JPAQueryFactory queryFactory;

    QActivity activity = QActivity.activity;

    @Override
    public GetHobbyActivitiesResDto getHobbyActivities(Hobby hobby) {
        List<GetHobbyActivitiesResDto.ActivityDto> activities = queryFactory
                .select(Projections.constructor(GetHobbyActivitiesResDto.ActivityDto.class,
                        activity.id,
                        activity.content,
                        activity.aiRecommended
                ))
                .from(activity)
                .where(activity.hobby.eq(hobby))
                .orderBy(activity.lastRecordedAt.desc().nullsLast(),
                        activity.lastRecordedAt.desc(),
                        activity.collectedStickerNum.desc(),
                        activity.content.asc()
                )
                .fetch();

        return new GetHobbyActivitiesResDto(activities);
    }
}
