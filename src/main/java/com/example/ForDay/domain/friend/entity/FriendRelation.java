package com.example.ForDay.domain.friend.entity;

import com.example.ForDay.domain.friend.type.FriendRelationStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "friend_relations")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendRelation extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "friend_relation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false)
    private User targetUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_status", nullable = false, length = 20)
    private FriendRelationStatus relationStatus;
}
