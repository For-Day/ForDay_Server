package com.example.ForDay.domain.hobby.entity;

import com.example.ForDay.domain.hobby.dto.request.HobbyCreateReqDto;
import com.example.ForDay.domain.hobby.dto.request.HobbyCreateReqDtoV2;
import com.example.ForDay.domain.hobby.dto.request.UpdateHobbyReqDto;
import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.util.Objects;


@Entity
@Table(
        name = "user_hobbies",
        indexes = {
                @Index(
                        name = "idx_hobby_user_status_created",
                        columnList = "user_id, status, created_at DESC"
                )
        }
)
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

    @Column(name = "hobbyPurpose", length = 50)
    private String hobbyPurpose;

    @Column(name = "hobby_time_minutes")
    private Integer hobbyTimeMinutes;

    @Column(name = "execution_count")
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

    @Column(name = "sequence")
    private Integer sequence;

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

    public void updateHobby(UpdateHobbyReqDto reqDto, Integer goalDays) {
        this.hobbyInfoId = reqDto.getHobbyInfoId();
        this.hobbyName = reqDto.getHobbyName();
        this.hobbyPurpose = reqDto.getHobbyPurpose();
        this.hobbyTimeMinutes = reqDto.getHobbyTimeMinutes();
        this.executionCount = reqDto.getExecutionCount();
        this.goalDays = goalDays;
    }

    public boolean isStickerFull() {
        final Integer STICKER_COMPLETE_COUNT = 66;

        if (this.currentStickerNum == null || this.goalDays == null) {
            return false;
        }
        return Objects.equals(this.currentStickerNum.intValue(), STICKER_COMPLETE_COUNT)
                && Objects.equals(this.goalDays.intValue(), STICKER_COMPLETE_COUNT);
    }

    public void validateCanRecord() {
        if (this.status != HobbyStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.INVALID_HOBBY_STATUS);
        }
        if (this.isStickerFull()) {
            throw new CustomException(ErrorCode.STICKER_COMPLETION_REACHED);
        }
    }

    public void validateInProgress() {
        if (this.status != HobbyStatus.IN_PROGRESS) {
            throw new CustomException(ErrorCode.INVALID_HOBBY_STATUS);
        }
    }

    public static Hobby createNewHobby(User user, HobbyCreateReqDto reqDto, Integer defaultGoalDays) {
        return Hobby.builder()
                .user(user)
                .hobbyInfoId(reqDto.getHobbyInfoId())
                .hobbyName(reqDto.getHobbyName())
                .hobbyPurpose(reqDto.getHobbyPurpose())
                .hobbyTimeMinutes(reqDto.getHobbyTimeMinutes())
                .executionCount(reqDto.getExecutionCount())
                .goalDays(reqDto.getIsDurationSet() ? defaultGoalDays : null)
                .status(HobbyStatus.IN_PROGRESS)
                .build();
    }

    public static Hobby createNewHobbyV2(User currentUser, HobbyCreateReqDtoV2.HobbyInfo info, int sequence) {
        return Hobby.builder()
                .user(currentUser)
                .hobbyInfoId(info.getHobbyInfoId())
                .hobbyName(info.getHobbyName())
                .status(HobbyStatus.IN_PROGRESS)
                .sequence(sequence)
                .build();
    }

    public void updateOrderAndStatus(Integer sequence, HobbyStatus status) {
        this.sequence = sequence;
        this.status = status;
    }
}
