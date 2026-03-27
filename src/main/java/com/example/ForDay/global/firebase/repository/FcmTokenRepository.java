package com.example.ForDay.global.firebase.repository;

import com.example.ForDay.global.firebase.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    List<FcmToken> findAllByUserIdAndIsRecordPushEnabledTrue(String userId);
    Optional<FcmToken> findByUserIdAndDeviceId(String userId, String deviceId);
}