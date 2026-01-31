package com.example.ForDay.domain.record.entity;

import com.example.ForDay.domain.activity.entity.Activity;
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
@Table(name = "activity_records")
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

    @Column(nullable = false, length = 20)
    private String sticker;

    @Column(length = 100)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecordVisibility visibility;

    private String imageUrl;

    @Builder.Default
    @OneToMany(mappedBy = "activityRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityRecordReaction> reactions = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "reportedRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityRecordReport> reports = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "activityRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityRecordScarp> scraps = new ArrayList<>();

    public void updateVisibility(RecordVisibility newVisibility) {
        this.visibility = newVisibility;
    }

    public void updateRecord(Activity activity, String sticker, String memo, RecordVisibility visibility, String imageUrl) {
        this.activity = activity;
        this.sticker = sticker;
        this.memo = memo;
        this.visibility = visibility;
        this.imageUrl = imageUrl;
    }
}
