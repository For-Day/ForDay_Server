package com.example.ForDay.global.firebase.service;

import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.firebase.dto.request.FcmNotificationReqDto;
import com.example.ForDay.domain.app.dto.response.UpdateFcmTokenResDto;
import com.example.ForDay.global.firebase.entity.FcmToken;
import com.example.ForDay.global.firebase.repository.FcmTokenRepository;
import com.example.ForDay.global.firebase.type.DeviceType;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmTokenService {
    private final FirebaseMessaging firebaseMessaging;
    private final FcmTokenRepository fcmTokenRepository;

    public String sendNotificationByToken(FcmNotificationReqDto reqDto) {
        if (existsFcmToken(reqDto.getFcmToken())) {
            Notification notification = Notification.builder()
                    .setTitle(reqDto.getTitle())
                    .setBody(reqDto.getBody())
                    //.setImage(reqDto.getImage())
                    .build();

            Message message = Message.builder()
                    .setToken(reqDto.getFcmToken())
                    .setNotification(notification)
                    .putAllData(reqDto.getData())
                    .build();

            try {
                firebaseMessaging.send(message);
                return "알림을 성공적으로 전송했습니다. fcmToken= " + reqDto.getFcmToken();
            } catch (FirebaseMessagingException e) {
                log.error(e.getMessage());
                return "알림 보내기를 실패하였습니다. fcmToken= " + reqDto.getFcmToken();
            }
        } else {
            return "서버에 저장된 유저의 FirebaseToken이 존재하지 않습니다. fcmToken= " + reqDto.getFcmToken();
        }
    }

    // 로그인 % 계정 전환시 fcmToken을 등록하기 위한 메서드
    @Transactional
    public String registerFcmToken(User user, String fcmToken, String deviceId, DeviceType deviceType) {
        if (fcmToken == null || deviceId == null) return null;

        FcmToken savedToken = fcmTokenRepository
                .findByUserIdAndDeviceId(user.getId(), deviceId)
                .map(token -> {
                    if (!isSameToken(fcmToken, token)) {
                        token.updateToken(fcmToken);
                    }
                    return token;
                })
                .orElseGet(() ->
                        fcmTokenRepository.save(FcmToken.createFcmToken(deviceId, user, fcmToken, deviceType))
                );
        return savedToken.getFcmToken();
    }

    @Transactional
    public UpdateFcmTokenResDto updateFcmToken(User user, String deviceId, String newFcmToken) {
        FcmToken fcmToken = fcmTokenRepository.findByUserIdAndDeviceId(user.getId(), deviceId)
                .orElseThrow(() -> new CustomException(ErrorCode.FCM_TOKEN_NOT_FOUND));

        if (!isSameToken(newFcmToken, fcmToken)) {
            fcmToken.updateToken(newFcmToken);
        }

        return UpdateFcmTokenResDto.of(deviceId, newFcmToken);
    }

    private static boolean isSameToken(String fcmToken, FcmToken token) {
        return Objects.equals(token.getFcmToken(), fcmToken);
    }

    private static boolean existsFcmToken(String fcmToken) {
        return StringUtils.hasText(fcmToken);
    }

    public List<FcmToken> findUserFcmToken(String userId) {
        return fcmTokenRepository.findByUserId(userId);
    }

    public void deleteUserFcmToken(List<FcmToken> userFcmToken) {
        if (userFcmToken != null && !userFcmToken.isEmpty()) {
            fcmTokenRepository.deleteAll(userFcmToken);
        }
    }
}