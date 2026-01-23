package com.example.ForDay.domain.record.repository;

import com.example.ForDay.domain.record.type.RecordReactionType;

import java.util.List;

public interface ActivityRecordReactionRepositoryCustom {
    List<RecordReactionType> findAllMyReactions(Long activityRecordId, String currentUserId);

    List<RecordReactionType> findAllUnreadReactions(Long activityRecordId);
}
