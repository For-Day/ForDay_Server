package com.example.ForDay.domain.notification.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetNotificationListResDto {
    private List<GetNotificationInfoResDto> notificationList;
    private boolean hasNext;
    private String lastNotificationId;
}
