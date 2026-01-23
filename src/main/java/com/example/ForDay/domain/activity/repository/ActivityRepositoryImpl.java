package com.example.ForDay.domain.activity.repository;

import com.example.ForDay.domain.activity.entity.QActivity;
import com.example.ForDay.domain.hobby.dto.response.GetActivityListResDto;
import com.example.ForDay.domain.hobby.dto.response.GetHobbyActivitiesResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.record.entity.QActivityRecord;
import com.example.ForDay.domain.user.entity.User;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ActivityRepositoryImpl implements ActivityRepositoryCustom{
    private final JPAQueryFactory queryFactory;

    QActivity activity = QActivity.activity;
    QActivityRecord activityRecord = QActivityRecord.activityRecord;

    @Override
    public GetHobbyActivitiesResDto getHobbyActivities(Hobby hobby, Integer size) {

        JPAQuery<GetHobbyActivitiesResDto.ActivityDto> query = queryFactory
                .select(Projections.constructor(
                        GetHobbyActivitiesResDto.ActivityDto.class,
                        activity.id,
                        activity.content,
                        activity.aiRecommended
                ))
                .from(activity)
                .where(activity.hobby.eq(hobby))
                .orderBy(
                        activity.lastRecordedAt.desc().nullsLast(),
                        activity.lastRecordedAt.desc(),
                        activity.collectedStickerNum.desc(),
                        activity.content.asc()
                );

        if (size != null) {
            query.limit(size);
        }

        return new GetHobbyActivitiesResDto(query.fetch());
    }


    @Override
    public GetActivityListResDto getActivityList(Hobby hobby, User currentUser) {

        List<GetActivityListResDto.ActivityDto> activities =
                queryFactory
                        .select(
                                Projections.constructor(
                                        GetActivityListResDto.ActivityDto.class,
                                        activity.id,
                                        activity.content,
                                        activity.aiRecommended,
                                        activity.collectedStickerNum
                                )
                        )
                        .from(activity)
                        .where(
                                activity.hobby.eq(hobby),
                                activity.user.eq(currentUser)
                        )
                        .orderBy(
                                activity.lastRecordedAt.desc().nullsLast(),
                                activity.collectedStickerNum.desc(),
                                activity.content.asc()
                        )
                        .fetch();


        if (activities.isEmpty()) {
            return new GetActivityListResDto(List.of());
        }

        return new GetActivityListResDto(activities);
    }
}

