package com.example.ForDay.domain.activity.entity;

import com.example.ForDay.domain.activity.dto.ActivityRecordCollectInfo;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@DynamicUpdate
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

    @Column(nullable = false, length = 50)
    private String content;

    private boolean aiRecommended;

    @Column(name = "collected_sticker_num")
    @Builder.Default
    private Integer collectedStickerNum = 0;

    @Column(name = "last_recorded_at")
    @Builder.Default
    private LocalDateTime lastRecordedAt = null;

    public static Activity from(User user, Hobby hobby, ActivityRecordCollectInfo info) {
        return Activity.builder()
                .user(user)
                .hobby(hobby)
                .content(info.getContent())
                .aiRecommended(false)
                .build();
    }

    public void record() {
        this.collectedStickerNum++;
        this.lastRecordedAt = LocalDateTime.now();
        hobby.record();
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void validateDeletable() {
        if (collectedStickerNum != 0) {
            throw new CustomException(ErrorCode.ACTIVITY_NOT_DELETABLE);
        }
    }

    public boolean isDeletable() {
        return collectedStickerNum == 0;
    }

    public void deleteRecord() {
        if(collectedStickerNum > 0) {
            this.collectedStickerNum--;
        }
    }
}
