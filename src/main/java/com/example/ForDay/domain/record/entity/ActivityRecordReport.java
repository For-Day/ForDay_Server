package com.example.ForDay.domain.record.entity;

import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "activity_record_reports")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityRecordReport extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_record_report_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private User reportedUser;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_record_id", nullable = false)
    private ActivityRecord reportedRecord;

    private String reason;
}
