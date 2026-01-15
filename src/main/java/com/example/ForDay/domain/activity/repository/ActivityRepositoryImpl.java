package com.example.ForDay.domain.activity.repository;

import com.example.ForDay.domain.activity.dto.StickerWithActivityIdDto;
import com.example.ForDay.domain.activity.entity.QActivity;
import com.example.ForDay.domain.activity.entity.QActivityRecord;
import com.example.ForDay.domain.hobby.dto.response.GetActivityListResDto;
import com.example.ForDay.domain.hobby.dto.response.GetHobbyActivitiesResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.user.entity.User;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class ActivityRepositoryImpl implements ActivityRepositoryCustom{
    private final JPAQueryFactory queryFactory;

    QActivity activity = QActivity.activity;
    QActivityRecord activityRecord = QActivityRecord.activityRecord;

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
                                        activity.deletable,
                                        Expressions.nullExpression(List.class)
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

        // 2. Activity ID 목록 추출
        List<Long> activityIds = activities.stream()
                .map(GetActivityListResDto.ActivityDto::getActivityId)
                .toList();

        // 3. Sticker 조회 (Activity 기준)
        List<StickerWithActivityIdDto> stickerResults =
                queryFactory
                        .select(
                                Projections.constructor(
                                        StickerWithActivityIdDto.class,
                                        activityRecord.activity.id,
                                        activityRecord.id,
                                        activityRecord.sticker
                                )
                        )
                        .from(activityRecord)
                        .where(
                                activityRecord.activity.id.in(activityIds),
                                activityRecord.user.eq(currentUser)
                        )
                        .orderBy(activityRecord.createdAt.asc())
                        .fetch();

        // 4. Activity ID 기준으로 Sticker 그룹핑
        Map<Long, List<GetActivityListResDto.StickerDto>> stickerMap =
                stickerResults.stream()
                        .collect(Collectors.groupingBy(
                                StickerWithActivityIdDto::getActivityId,
                                Collectors.mapping(
                                        s -> new GetActivityListResDto.StickerDto(
                                                s.getActivityRecordId(),
                                                s.getSticker()
                                        ),
                                        Collectors.toList()
                                )
                        ));

        // 5. ActivityDto에 Sticker 주입
        activities.forEach(activityDto ->
                activityDto.setStickers(
                        stickerMap.getOrDefault(activityDto.getActivityId(), List.of())
                )
        );

        return new GetActivityListResDto(activities);
    }
}

