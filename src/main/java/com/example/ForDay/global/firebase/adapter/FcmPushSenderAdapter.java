package com.example.ForDay.global.firebase.adapter;

import com.example.ForDay.global.firebase.dto.request.FcmNotificationReqDto;
import com.example.ForDay.global.firebase.service.FcmTokenService;
import com.example.ForDay.global.port.PushMessage;
import com.example.ForDay.global.port.PushSenderPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * FCM으로 푸시를 보낸다. Firebase 타입은 이 경로 밖으로 나가지 않는다.
 */
@Component
@RequiredArgsConstructor
public class FcmPushSenderAdapter implements PushSenderPort {

    private final FcmTokenService fcmTokenService;

    @Override
    public void send(PushMessage message) {
        fcmTokenService.sendNotificationByToken(new FcmNotificationReqDto(
                message.deviceToken(),
                message.title(),
                message.body(),
                null,
                message.data()
        ));
    }
}
