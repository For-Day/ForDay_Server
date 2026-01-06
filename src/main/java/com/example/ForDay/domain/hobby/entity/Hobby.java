package com.example.ForDay.domain.hobby.entity;

import com.example.ForDay.domain.hobby.type.HobbyStatus;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
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

    @Column(name = "hobby_time_minutes", nullable = false)
    private Integer hobbyTimeMinutes;

    @Column(name = "execution_count", nullable = false)
    private Integer executionCount;

    @Column(name = "current_grapes")
    @Builder.Default
    private Integer currentGrapes = 0;

    @Column(name = "goal_grapes")
    @Builder.Default
    private Integer goalGrapes = null;

    @Column(name = "goal_days")
    @Builder.Default
    private Integer goalDays = null;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private HobbyStatus status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @OneToMany(
            mappedBy = "hobby",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<HobbyPurpose> purposes = new ArrayList<>();

    public void addPurpose(HobbyPurpose purpose) {
        purposes.add(purpose);
        purpose.setHobby(this);
    }

    public void removePurpose(HobbyPurpose purpose) {
        purposes.remove(purpose);
        purpose.setHobby(null);
    }
}
