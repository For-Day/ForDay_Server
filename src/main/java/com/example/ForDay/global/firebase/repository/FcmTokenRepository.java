package com.example.ForDay.global.firebase.repository;

import com.example.ForDay.global.firebase.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    Optional<FcmToken> findByUserIdAndDeviceId(String userId, String deviceId);
    List<FcmToken> findByUserId(String userId);

    Optional<FcmToken> findByFcmToken(String fcmToken);
}