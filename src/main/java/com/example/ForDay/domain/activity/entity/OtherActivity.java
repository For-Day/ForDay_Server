package com.example.ForDay.domain.activity.entity;

import com.example.ForDay.domain.hobby.entity.HobbyInfo;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "other_activities")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtherActivity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "other_activity_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hobby_info_id", nullable = false)
    private HobbyInfo hobbyInfo;

    @Column(name = "time_minutes", nullable = false)
    private Integer timeMinutes;

    private String purpose;

    private String content;
}
