package com.example.ForDay.domain.reaction.entity;

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
}
