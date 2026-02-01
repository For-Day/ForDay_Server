package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.entity.QActivityRecord;
import com.example.ForDay.domain.record.entity.QActivityRecordScarp;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.user.dto.response.GetUserScrapListResDto;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ActivityRecordScrapRepositoryImpl implements ActivityRecordScrapRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final QActivityRecord record = QActivityRecord.activityRecord;
    private final QActivityRecordScarp scrap = QActivityRecordScarp.activityRecordScarp;

    @Override
    public List<GetUserScrapListResDto.ScrapDto> getMyScrapList(Long lastScrapId, Integer size, String targetUserId) {
        return queryFactory
                .select(Projections.constructor(
                        GetUserScrapListResDto.ScrapDto.class,
                        scrap.id,
                        record.id,
                        record.imageUrl,
                        record.sticker,
                        record.memo,
                        scrap.createdAt
                ))
                .from(scrap)
                .join(scrap.activityRecord, record)
                .where(
                        ltLastScrapId(lastScrapId),
                        scrap.user.id.eq(targetUserId),
                        record.deleted.isFalse()
                )
                .orderBy(scrap.id.desc())
                .limit(size + 1)
                .fetch();
    }

    public List<GetUserScrapListResDto.ScrapDto> getOtherScrapList(Long lastScrapId, Integer size, String targetUserId, String currentUserId, List<String> myFriendIds, List<String> blockFriendIds, List<Long> reportedRecordIds) {
        return queryFactory
                .select(Projections.constructor(GetUserScrapListResDto.ScrapDto.class,
                        scrap.id,
                        record.id,
                        record.imageUrl,
                        record.sticker,
                        record.memo,
                        scrap.createdAt
                ))
                .from(scrap)
                .join(scrap.activityRecord, record)
                .where(
                        scrap.user.id.eq(targetUserId),          // 1. 조회 대상 유저의 스크랩
                        ltLastScrapId(lastScrapId),              // 2. No-offset 페이징
                        record.user.deleted.isFalse(),         // 3. 탈퇴한 유저 제외
                        record.user.id.notIn(blockFriendIds),   // 4. 차단 관계 유저 제외
                        record.deleted.isFalse(), // 5. 삭제 안된 기록 조회
                        // 6. 공개 범위 및 본인/친구 권한 체크
                        record.visibility.eq(RecordVisibility.PUBLIC)
                                .or(
                                        record.visibility.eq(RecordVisibility.FRIEND)
                                                .and(
                                                        record.user.id.in(myFriendIds)     // 내 친구이거나
                                                                .or(record.user.id.eq(currentUserId)) // 나 자신인 경우
                                                )
                                ),
                        notInReportedList(reportedRecordIds)
                )
                .orderBy(scrap.id.desc())
                .limit(size + 1)
                .fetch();
    }

    private BooleanExpression notInReportedList(List<Long> reportedRecordIds) {
        if (reportedRecordIds == null || reportedRecordIds.isEmpty()) {
            return null; // 조건 무시
        }
        return record.id.notIn(reportedRecordIds);
    }

    private BooleanExpression ltLastScrapId(Long lastScrapId) {
        return lastScrapId != null ? scrap.id.lt(lastScrapId) : null;
    }
}
