package com.example.ForDay.domain.notification.controller;

import com.example.ForDay.domain.notification.dto.request.UpdatePushNotificationToggleReqDto;
import com.example.ForDay.domain.notification.dto.response.GetNotificationListResDto;
import com.example.ForDay.domain.notification.dto.response.GetPushNotificationToggleResDto;
import com.example.ForDay.domain.notification.dto.response.UpdatePushNotificationToggleResDto;
import com.example.ForDay.domain.notification.service.NotificationService;
import com.example.ForDay.domain.notification.type.NotificationFilterType;
import com.example.ForDay.global.oauth.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public GetNotificationListResDto getNotificationList(@RequestParam(name = "filterType", required = false) NotificationFilterType filterType,
                                                         @RequestParam(name = "lastNotificationId", required = false) Long lastNotificationId,
                                                         @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize,
                                                         @AuthenticationPrincipal CustomUserDetails user) {

        return notificationService.getNotificationList(filterType, lastNotificationId, pageSize, user.getUser());
    }

    @PatchMapping("/toggle")
    public UpdatePushNotificationToggleResDto updatePushNotificationToggle(@RequestBody @Valid UpdatePushNotificationToggleReqDto reqDto,
                                                                           @AuthenticationPrincipal CustomUserDetails user) {
        return notificationService.updatePushNotificationToggle(reqDto, user);
    }

    @GetMapping("/toggle")
    public GetPushNotificationToggleResDto getPushNotificationToggle(@RequestParam(name = "deviceId") String deviceId,
                                                                     @AuthenticationPrincipal CustomUserDetails user) {
        return notificationService.getPushNotificationToggle(deviceId, user);
    }
}