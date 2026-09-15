package com.example.ForDay.domain.reaction.dto;

import com.example.ForDay.domain.record.type.RecordReactionType;

/**
 * (recordId, userId, type) 조합의 유니크 키만 담는 조회 전용 DTO.
 * ReactionScheduler가 벌크 저장 전에 이미 존재하는 반응을 한 번의 쿼리로 걸러내는 데 사용한다.
 */
public record ReactionKeyDto(Long recordId, String userId, RecordReactionType type) {

    public String toKey() {
        return recordId + ":" + userId + ":" + type;
    }
}
