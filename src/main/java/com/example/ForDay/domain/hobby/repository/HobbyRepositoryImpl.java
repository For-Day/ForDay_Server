package com.example.ForDay.domain.hobby.repository;

import com.example.ForDay.domain.activity.entity.QActivity;
import com.example.ForDay.domain.activity.entity.QActivityRecord;
import com.example.ForDay.domain.auth.dto.response.OnboardingDataDto;
import com.example.ForDay.domain.hobby.dto.response.GetHomeHobbyInfoResDto;
import com.example.ForDay.domain.hobby.dto.response.MyHobbySettingResDto;
import com.example.ForDay.domain.hobby.entity.QHobby;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.ai.service.AiCallCountService;
import com.example.ForDay.global.util.RedisUtil;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class HobbyRepositoryImpl implements HobbyRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final RedisUtil redisUtil;
    private final AiCallCountService aiCallCountService;

    QHobby hobby = QHobby.hobby;
    QActivity activity = QActivity.activity;
    QActivityRecord activityRecord = QActivityRecord.activityRecord;

    @Override
    public GetHomeHobbyInfoResDto getHomeHobbyInfo(Long hobbyId, User currentUser) {

        // 1. Hobby 리스트 조회 (Expressions.constant 사용)
        List<GetHomeHobbyInfoResDto.InProgressHobbyDto> hobbyList = queryFactory
                .select(Projections.constructor(GetHomeHobbyInfoResDto.InProgressHobbyDto.class,
                        hobby.id,
                        hobby.hobbyName,
                        Expressions.constant(false)
                ))
                .from(hobby)
                .where(hobby.user.eq(currentUser), hobby.status.eq(HobbyStatus.IN_PROGRESS))
                .orderBy(hobby.createdAt.desc())
                .fetch();

        if (hobbyList.isEmpty()) return null;

        // 2. targetHobbyId 결정 및 마킹
        Long targetHobbyId = (hobbyId == null) ? hobbyList.get(0).getHobbyId() : hobbyId;

        hobbyList.forEach(h -> {
            if (h.getHobbyId().equals(targetHobbyId)) {
                h.setCurrentHobby(true);
            }
        });

        // 3. Activity Preview 조회
        GetHomeHobbyInfoResDto.ActivityPreviewDto activityPreview = queryFactory
                .select(Projections.constructor(GetHomeHobbyInfoResDto.ActivityPreviewDto.class,
                        activity.id,
                        activity.content,
                        activity.aiRecommended
                ))
                .from(activity)
                .where(activity.hobby.id.eq(targetHobbyId))
                .orderBy(
                        activity.lastRecordedAt.desc().nullsLast(),
                        activity.collectedStickerNum.desc(),
                        activity.content.asc()
                )
                .fetchFirst();



        boolean isAiCallRemaining = true;
        int currentCount = aiCallCountService.getCurrentCount(currentUser.getSocialId(), targetHobbyId);
        if(currentCount >= 3) isAiCallRemaining = false;

        return GetHomeHobbyInfoResDto.builder()
                .inProgressHobbies(hobbyList)
                .activityPreview(activityPreview)
                .aiCallRemaining(isAiCallRemaining)
                .build();
    }

    @Override
    public MyHobbySettingResDto myHobbySetting(User user, HobbyStatus hobbyStatus) {
        List<MyHobbySettingResDto.HobbyDto> hobbyDtos = queryFactory
                .select(Projections.constructor(MyHobbySettingResDto.HobbyDto.class,
                        hobby.id,
                        hobby.hobbyName,
                        hobby.hobbyTimeMinutes,
                        hobby.executionCount,
                        hobby.goalDays
                ))
                .from(hobby)
                .where(
                        hobby.user.eq(user),
                        hobby.status.eq(hobbyStatus)
                )
                .orderBy(hobby.createdAt.desc())
                .fetch();

        Long inProgressHobbyCount = queryFactory
                .select(hobby.count())
                .from(hobby)
                .where(hobby.user.eq(user),
                        hobby.status.eq(HobbyStatus.IN_PROGRESS))
                .fetchOne();

        Long archivedHobbyCount = queryFactory
                .select(hobby.count())
                .from(hobby)
                .where(hobby.user.eq(user),
                        hobby.status.eq(HobbyStatus.ARCHIVED))
                .fetchOne();

        return new MyHobbySettingResDto(hobbyStatus, inProgressHobbyCount, archivedHobbyCount ,hobbyDtos);
    }

    @Override
    public OnboardingDataDto getOnboardingDate(User user) {
        return queryFactory
                .select(Projections.constructor(OnboardingDataDto.class,
                        hobby.id,
                        hobby.hobbyInfoId,
                        hobby.hobbyName,
                        hobby.hobbyPurpose,
                        hobby.hobbyTimeMinutes,
                        hobby.executionCount,
                        hobby.goalDays
                        )
                )
                .from(hobby)
                .where(hobby.user.eq(user))
                .orderBy(hobby.createdAt.asc())
                .fetchFirst();
    }
}