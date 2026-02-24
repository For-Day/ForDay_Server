package com.example.ForDay.domain.app.entity;

import com.example.ForDay.domain.app.type.Platform;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_version") // 테이블명은 서비스에 맞춰 변경하세요
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AppVersion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Platform platform;

    @Column(name = "policy_version", nullable = false)
    private Integer policyVersion;

    @Column(name = "min_supported_version", nullable = false, length = 20)
    private String minSupportedVersion;

    @Column(name = "min_supported_build", nullable = false)
    private Integer minSupportedBuild;

    @Column(name = "latest_version", nullable = false, length = 20)
    private String latestVersion;

    @Column(name = "latest_build", nullable = false)
    private Integer latestBuild;

    @Column(name = "store_url", nullable = false, length = 255)
    private String storeUrl;

    @Column(name = "force_message", nullable = false, length = 255)
    private String forceMessage; // 강제 업데이트 문구

    @Column(name = "recommend_message", nullable = false, length = 255)
    private String recommendMessage; // 권장 업데이트 문구

    @Column(name = "block_enabled", nullable = false)
    @Builder.Default
    private Boolean blockEnabled = false; // 서비스 차단 여부

    @Column(name = "block_message", length = 255)
    private String blockMessage; // 차단 문구

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public boolean isServiceBlocked() {
        return Boolean.TRUE.equals(this.blockEnabled);
    }
}