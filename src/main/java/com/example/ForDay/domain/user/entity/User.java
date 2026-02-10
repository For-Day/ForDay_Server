package com.example.ForDay.domain.user.entity;

import com.example.ForDay.domain.user.type.Role;
import com.example.ForDay.domain.user.type.SocialType;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "uk_users_nickname", columnList = "nickname", unique = true),
                @Index(name = "uk_users_social_id", columnList = "social_id", unique = true)
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "user_id", columnDefinition = "CHAR(36)")
    private String id;

    @Column(name = "email", length = 50)
    @Builder.Default
    private String email = null;

    @Column(name = "nickname", length = 50, unique = true)
    @Builder.Default
    private String nickname = null;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_type", length = 20, nullable = false)
    private SocialType socialType;

    @Column(name = "social_id", unique = true, nullable = false)
    private String socialId;

    @Column(name = "last_activity_at")
    @Builder.Default
    private LocalDateTime lastActivityAt = null; // 게스트용 마지막 활동일시 저장

    @Builder.Default
    private boolean onboardingCompleted = false;

    @Column(name = "profile_image_url")
    @Builder.Default
    private String profileImageUrl = null;

    @Builder.Default
    private Integer totalCollectedStickerCount = 0;

    @Builder.Default
    private Integer hobbyCardCount = 0;

    @Builder.Default
    private boolean deleted = false;

    @Builder.Default
    private LocalDateTime deletedAt = null;

    // 게스트 마지막 활동일시 업데이트
    public void updateLastActivity() {
        this.lastActivityAt = LocalDateTime.now();
    }

    // 닉네임 변경
    public void changeNickname(String nickname) {
        this.nickname = nickname.trim();
    }

    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }

    public void obtainSticker() {
        this.totalCollectedStickerCount++;
    }

    public void updateProfileImage(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public Integer getTotalCollectedStickerCount() {
        return totalCollectedStickerCount == null ? 0 : totalCollectedStickerCount;
    }

    public void obtainHobbyCard() {
        this.hobbyCardCount++;
    }

    public void switchAccount(String email, Role role, SocialType socialType, String socialId) {
        this.email = email;
        this.role = role;
        this.socialType = socialType;
        this.socialId = socialId;
    }

    public void withdraw() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();

        this.email = null;

        // 닉네임 유니크 해제 (재가입 시 중복 방지)
        if (this.nickname != null) {
            this.nickname = "WITHDRAWN_" + this.id.substring(0, 8);
        }

        if (this.socialId != null && !this.socialId.startsWith("withdrawn_")) {
            this.socialId = "withdrawn_" + this.socialId;
        }

        this.profileImageUrl = null;
    }
}
