package com.example.ForDay.global.firebase.entity;

import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.mapped.BaseTimeEntity;
import com.example.ForDay.global.firebase.type.DeviceType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Table(name = "fcm_tokens")
public class FcmToken extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fcm_token_id")
    private Long id;

    @Column(nullable = false)
    private String deviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    private DeviceType deviceType;

    @Column(nullable = false)
    @Builder.Default
    private boolean isAppPushEnabled = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean isRecordPushEnabled = false;

    public static FcmToken createFcmToken(String deviceId, User user, String fcmToken, DeviceType deviceType) {
        return FcmToken.builder()
                .deviceId(deviceId)
                .user(user)
                .fcmToken(fcmToken)
                .deviceType(deviceType)
                .build();
    }

    public void updateToken(String newToken) {
        this.fcmToken = newToken;
    }

    public void updateAppPushEnabled(boolean active) {
        this.isAppPushEnabled = active;
    }

    public void updateRecordPushEnabled(boolean active) {
        this.isRecordPushEnabled = active;
    }
}