package com.example.ForDay.domain.activity.entity;

import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    private boolean aiRecommended;

    @Column(name = "collected_sticker_num")
    @Builder.Default
    private Integer collectedStickerNum = 0;

    @Column(name = "last_recorded_at")
    @Builder.Default
    private LocalDateTime lastRecordedAt = null;

    public void record() {
        this.collectedStickerNum++;
        this.lastRecordedAt = LocalDateTime.now();
        hobby.record();
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public boolean isDeletable() {
        return collectedStickerNum == 0; // 해당 활동에 대한 활동 기록이 없으면 삭제 가능
    }

    public void deleteRecord() {
        if(collectedStickerNum > 0) {
            this.collectedStickerNum--;
        }
    }
}
