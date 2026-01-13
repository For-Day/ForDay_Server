package com.example.ForDay.domain.hobby.entity;

import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_hobbies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Hobby extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_hobby_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "hobby_card_id")
    private Long hobbyCardId;

    @Column(name = "hobby_name", nullable = false, length = 50)
    private String hobbyName;

    @Column(name = "hobbyPurpose", nullable = false, length = 50)
    private String hobbyPurpose;

    @Column(name = "hobby_time_minutes", nullable = false)
    private Integer hobbyTimeMinutes;

    @Column(name = "execution_count", nullable = false)
    private Integer executionCount;

    @Column(name = "current_sticker_num")
    @Builder.Default
    private Integer currentStickerNum = 0;

    @Column(name = "goal_sticker_num")
    @Builder.Default
    private Integer goalStickerNum = null; // 나중에 삭제될 수도

    @Column(name = "goal_days")
    @Builder.Default
    private Integer goalDays = null; // 기간 미지정이면 null, 기간 지정이면 66

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private HobbyStatus status;


    public void record() {
        this.currentStickerNum++;
    }
}
