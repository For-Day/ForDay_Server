package com.example.ForDay.domain.activity.repository;

import com.example.ForDay.domain.activity.dto.ActivityRecordCollectInfo;
import com.example.ForDay.domain.activity.entity.QActivity;
import com.example.ForDay.domain.hobby.dto.response.GetActivityListResDto;
import com.example.ForDay.domain.hobby.dto.response.GetHobbyActivitiesResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.record.dto.ReportActivityRecordDto;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.QActivityRecord;
import com.example.ForDay.domain.user.entity.User;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
public class ActivityRepositoryImpl implements ActivityRepositoryCustom{
    private final JPAQueryFactory queryFactory;

    QActivity activity = QActivity.activity;

    @Override
    public GetHobbyActivitiesResDto getHobbyActivities(Long hobbyId, Integer size) { // Hobby 객체 대신 ID만 받음

        JPAQuery<GetHobbyActivitiesResDto.ActivityDto> query = queryFactory
                .select(Projections.constructor(
                        GetHobbyActivitiesResDto.ActivityDto.class,
                        activity.id,
                        activity.content,
                        activity.aiRecommended
                ))
                .from(activity)
                .where(activity.hobby.id.eq(hobbyId))
                .orderBy(
                        activity.createdAt.desc(),
                        activity.lastRecordedAt.desc().nullsLast(),
                        activity.collectedStickerNum.desc(),
                        activity.content.asc()
                );

        if (size != null) {
            query.limit(size);
        }

        return new GetHobbyActivitiesResDto(query.fetch());
    }


    @Override
    public GetActivityListResDto getActivityList(Long hobbyId, String userId) {

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
                                activity.hobby.id.eq(hobbyId),
                                activity.user.id.eq(userId)
                        )
                        .orderBy(
                                activity.createdAt.desc(),
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

    @Override
    public Optional<ActivityRecordCollectInfo> getCollectActivityInfo(Long activityId) {
        return Optional.ofNullable(
                queryFactory
                        .select(Projections.constructor(ActivityRecordCollectInfo.class,
                                activity.user.id,
                                activity.user.deleted,
                                activity.content
                        ))
                        .from(activity)
                        .join(activity.user)
                        .where(activity.id.eq(activityId))
                        .fetchOne()
        );
    }
}

