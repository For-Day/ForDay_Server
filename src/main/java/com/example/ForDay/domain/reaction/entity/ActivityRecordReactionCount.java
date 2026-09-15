package com.example.ForDay.domain.reaction.entity;

import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "record_reaction_count")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityRecordReactionCount extends BaseTimeEntity {

    @Id
    @Column(name = "record_id")
    private Long recordId;

    private Long totalCount;
    private Long awesomeCount;
    private Long greatCount;
    private Long amazingCount;
    private Long fightingCount;

    public static ActivityRecordReactionCount init(Long recordId, RecordReactionType type) {
        return initWithCount(recordId, type, 1L);
    }

    // 스케줄러가 (recordId, type) 단위로 증가량을 그룹핑해 반영할 때, row가 아직 없는 조합을
    // 그룹 증가량(count)으로 바로 초기화하기 위한 팩토리 메서드.
    public static ActivityRecordReactionCount initWithCount(Long recordId, RecordReactionType type, long count) {
        return ActivityRecordReactionCount.builder()
                .recordId(recordId)
                .totalCount(count)
                .awesomeCount(type == RecordReactionType.AWESOME ? count : 0L)
                .greatCount(type == RecordReactionType.GREAT ? count : 0L)
                .amazingCount(type == RecordReactionType.AMAZING ? count : 0L)
                .fightingCount(type == RecordReactionType.FIGHTING ? count : 0L)
                .build();
    }
}
