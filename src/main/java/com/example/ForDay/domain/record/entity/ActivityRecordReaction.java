package com.example.ForDay.domain.record.entity;

import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_record_reactions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityRecordReaction extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_record_reaction_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_record_id", nullable = false)
    private ActivityRecord activityRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reacted_user_id", nullable = false)
    private User reactedUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordReactionType reactionType;

    @Builder.Default
    private boolean readWriter = false;
}
