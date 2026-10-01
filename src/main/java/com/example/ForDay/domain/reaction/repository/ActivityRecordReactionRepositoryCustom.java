package com.example.ForDay.domain.reaction.repository;

import com.example.ForDay.domain.reaction.dto.ReactionKeyDto;
import com.example.ForDay.domain.record.dto.response.GetRecordReactionUsersResDto;
import com.example.ForDay.domain.record.dto.response.ReactionSummaryResDto;
import com.example.ForDay.domain.record.dto.response.ReactionTabScrollResDto;
import com.example.ForDay.domain.record.type.RecordReactionType;

import java.util.List;
import java.util.Map;

public interface ActivityRecordReactionRepositoryCustom {
    List<RecordReactionType> findAllMyReactions(Long activityRecordId, String currentUserId);

    /**
     * (recordId, userId, type) 조합을 한 번의 다중 VALUES INSERT 문으로 저장한다.
     * {@link org.springframework.data.jpa.repository.JpaRepository#saveAll}은 JDBC 배치
     * 설정이 없으면 건당 INSERT를 그대로 보내므로, 대량 반응을 진짜 한 번에 반영하려면
     * 이 메서드를 쓴다. ReactionScheduler의 벌크 저장 경로 전용.
     */
    void bulkInsert(List<ReactionKeyDto> rows);

    List<RecordReactionType> findAllUnreadReactions(Long activityRecordId);

    List<GetRecordReactionUsersResDto.ReactionUserInfo> findReactionUsersDtoByType(Long recordId, RecordReactionType type, String lastUserId, Integer size, boolean isRecordOwner);

    Map<String, ReactionSummaryResDto.ReactionSliceDto> getReactionSummary(Long recordId, int size, String currentUserId);

    ReactionTabScrollResDto getReactionTabScroll(Long recordId, RecordReactionType type, Long lastReactionId, int size, String currentUserId);
}
