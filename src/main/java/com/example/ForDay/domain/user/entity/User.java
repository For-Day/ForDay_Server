package com.example.ForDay.domain.user.entity;

import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "uk_users_nickname", columnList = "nickname", unique = true)
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "email", length = 50)
    @Builder.Default
    private String email = null;

    @Column(name = "nickname", length = 10, unique = true)
    @Builder.Default
    private String nickname = null;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", length = 20, nullable = false)
    private SocialType socialType;

    @Column(name = "social_id", length = 100, unique = true, nullable = false)
    private String socialId;

    @Column(name = "last_routine_recorded_at")
    @Builder.Default
    private LocalDateTime lastRoutineRecordedAt = null;

    @Column(name = "last_activity_at")
    @Builder.Default
    private LocalDateTime lastActivityAt = null; // 게스트용 마지막 활동일시 저장

    // 게스트 마지막 활동일시 업데이트
    public void updateLastActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    // 닉네임 변경
    public void changeNickname(String nickname) {
        this.nickname = nickname.trim();
    }

    // 최근 루틴 기록 일시 업데이트
    public void updateLastRoutineRecordedAt() {
        this.lastRoutineRecordedAt = LocalDateTime.now();
    }

}
