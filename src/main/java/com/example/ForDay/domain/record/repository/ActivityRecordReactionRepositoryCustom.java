package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.dto.response.GetRecordReactionUsersResDto;
import com.example.ForDay.domain.record.entity.ActivityRecordReaction;
import com.example.ForDay.domain.record.type.RecordReactionType;

import java.util.List;

public interface ActivityRecordReactionRepositoryCustom {
    List<RecordReactionType> findAllMyReactions(Long activityRecordId, String currentUserId);

    List<RecordReactionType> findAllUnreadReactions(Long activityRecordId);

    List<GetRecordReactionUsersResDto.ReactionUserInfo> findReactionUsersDtoByType(Long recordId, RecordReactionType type, String lastUserId, Integer size, boolean isRecordOwner);
}
