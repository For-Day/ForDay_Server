package com.example.ForDay.domain.hobby.entity;

import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "user_hobbies")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@DynamicUpdate
public class Hobby extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_hobby_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "hobby_info_id")
    private Long hobbyInfoId;

    @Column(name = "hobby_name", nullable = false, length = 20)
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

    @Column(name = "goal_days")
    @Builder.Default
    private Integer goalDays = null; // 기간 미지정이면 null, 기간 지정이면 66

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private HobbyStatus status;

    @Builder.Default
    private String coverImageUrl = null;


    public void record() {
        this.currentStickerNum++;
    }

    public void updateHobbyTimeMinutes(Integer hobbyTimeMinutes) {
        this.hobbyTimeMinutes = hobbyTimeMinutes;
    }

    public void updateExecutionCount(Integer executionCount) {
        this.executionCount = executionCount;
    }

    public void updateGoalDays(Integer goalDays) {
        this.goalDays = goalDays;
    }

    public void updateHobbyStatus(HobbyStatus hobbyStatus) {
        this.status = hobbyStatus;
    }

    public boolean isUpdatable() {
        return this.status == HobbyStatus.IN_PROGRESS;
    }

    public void setHobbyArchived() {
        this.status = HobbyStatus.ARCHIVED;
    }

    public void setGoalDaysExtension() {
        this.goalDays = null;
    }

    public void updateCoverImage(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public void deleteRecord() {
        if(currentStickerNum > 0) {
            this.currentStickerNum--;
        }
    }

    public void updateHobby(Long hobbyInfoId, String hobbyName, String hobbyPurpose, Integer hobbyTimeMinutes, Integer executionCount, Integer goalDays) {
        this.hobbyInfoId = hobbyInfoId;
        this.hobbyName = hobbyName;
        this.hobbyPurpose = hobbyPurpose;
        this.hobbyTimeMinutes = hobbyTimeMinutes;
        this.executionCount = executionCount;
        this.goalDays = goalDays;
    }
}
