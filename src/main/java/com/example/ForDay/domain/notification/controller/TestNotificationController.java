package com.example.ForDay.domain.notification.controller;

import com.example.ForDay.domain.notification.dto.request.SendPushMessageReqDto;
import com.example.ForDay.domain.notification.dto.response.SendPushMessageResDto;
import com.example.ForDay.domain.notification.service.NotificationService;
import com.example.ForDay.global.oauth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class TestNotificationController {
    private final NotificationService notificationService;

    @PostMapping("/fcm/sendMessage")
    public SendPushMessageResDto sendPushMessage(@RequestBody SendPushMessageReqDto reqDto,
                                                 @AuthenticationPrincipal CustomUserDetails user) {

        return notificationService.sendPushMessage(reqDto, user);
    }
}