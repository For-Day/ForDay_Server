package com.example.ForDay.domain.activity.entity;

import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Activity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_activity_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_hobby_id", nullable = false)
    private Hobby hobby;

    @Column(nullable = false, length = 100)
    private String content;

    @Column(length = 255)
    private String description;

    private boolean aiRecommended;

    @Column(name = "collected_sticker_num")
    @Builder.Default
    private Integer collectedStickerNum = 0;
}
