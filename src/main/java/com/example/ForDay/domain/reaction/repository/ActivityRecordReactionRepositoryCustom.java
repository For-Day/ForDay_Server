package com.example.ForDay.domain.reaction.repository;

import com.example.ForDay.domain.record.dto.response.GetRecordReactionUsersResDto;
import com.example.ForDay.domain.record.dto.response.ReactionSummaryResDto;
import com.example.ForDay.domain.record.type.RecordReactionType;

import java.util.List;
import java.util.Map;

public interface ActivityRecordReactionRepositoryCustom {
    List<RecordReactionType> findAllMyReactions(Long activityRecordId, String currentUserId);

    List<RecordReactionType> findAllUnreadReactions(Long activityRecordId);

    List<GetRecordReactionUsersResDto.ReactionUserInfo> findReactionUsersDtoByType(Long recordId, RecordReactionType type, String lastUserId, Integer size, boolean isRecordOwner);

    Map<String, ReactionSummaryResDto.ReactionSliceDto> getReactionSummary(Long recordId, int size, String currentUserId);
}
