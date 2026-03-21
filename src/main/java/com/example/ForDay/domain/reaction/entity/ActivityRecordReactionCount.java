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
        return ActivityRecordReactionCount.builder()
                .recordId(recordId)
                .totalCount(1L)
                .awesomeCount(type == RecordReactionType.AWESOME ? 1L : 0L)
                .greatCount(type == RecordReactionType.GREAT ? 1L : 0L)
                .amazingCount(type == RecordReactionType.AMAZING ? 1L : 0L)
                .fightingCount(type == RecordReactionType.FIGHTING ? 1L : 0L)
                .build();
    }
}
