package com.example.ForDay.domain.notification.controller;

import com.example.ForDay.domain.notification.dto.response.GetNotificationListResDto;
import com.example.ForDay.domain.notification.service.NotificationService;
import com.example.ForDay.domain.notification.type.NotificationFilterType;
import com.example.ForDay.domain.notification.type.NotificationType;
import com.example.ForDay.global.oauth.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public GetNotificationListResDto getNotificationList(
            @RequestParam(name = "filterType", required = false) NotificationFilterType filterType,
            @RequestParam(name = "lastNotificationId", required = false) Long lastNotificationId,
            @RequestParam(name = "pageSize", defaultValue = "20") Integer pageSize,
            @AuthenticationPrincipal CustomUserDetails user) {

        return notificationService.getNotificationList(filterType, lastNotificationId, pageSize, user.getUser());
    }
}
