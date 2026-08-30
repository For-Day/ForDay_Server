package com.example.ForDay.domain.record.entity;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.reaction.entity.ActivityRecordReaction;
import com.example.ForDay.domain.record.command.RecordCreateCommand;
import com.example.ForDay.domain.record.command.RecordUpdateCommand;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "activity_records",
        indexes = {
                @Index(
                        name = "idx_ar_user_hobby_created",
                        columnList = "user_hobby_id, user_id, created_at DESC"
                )
        })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityRecord extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_record_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_hobby_id", nullable = false)
    private Hobby hobby;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 20)
    private String sticker;

    @Column(length = 200)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecordVisibility visibility;

    private String imageUrl;

    @Builder.Default
    private boolean deleted = false;

    @Builder.Default
    @OneToMany(mappedBy = "activityRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecordImage> images = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "activityRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityRecordReaction> reactions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "reportedRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityRecordReport> reports = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "activityRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityRecordScrap> scraps = new ArrayList<>();

    public void updateVisibility(RecordVisibility newVisibility) {
        this.visibility = newVisibility;
    }

    /**
     * DTO 의존을 걷어내니 V1·V2 본문이 같아져 하나로 합쳤다.
     * 대표 이미지를 무엇으로 볼지(V1은 단일 이미지, V2는 목록의 첫 장)는 호출부가 정한다.
     */
    public void updateRecord(Activity activity, RecordUpdateCommand command) {
        this.activity = activity;
        this.sticker = command.sticker();
        this.memo = command.memo();
        this.visibility = command.visibility();
        this.imageUrl = command.imageUrl();
    }

    public void deleteRecord() {
        this.sticker = null;
        this.memo = null;
        this.imageUrl = null;
        this.deleted = true;
    }

    /** {@link #updateRecord}와 같은 이유로 V1·V2 팩토리를 하나로 합쳤다. */
    public static ActivityRecord of(Hobby hobby, Activity activity, User user, RecordCreateCommand command) {
        return ActivityRecord.builder()
                .hobby(hobby)
                .activity(activity)
                .user(user)
                .sticker(command.sticker())
                .memo(command.memo())
                .visibility(command.visibility())
                .imageUrl(command.imageUrl())
                .build();
    }
}
