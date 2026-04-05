package com.example.ForDay.domain.notification.dto.response;

import com.example.ForDay.global.common.response.message.NotificationSuccessCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetNotificationListResDto {
    private PushInfo pushInfo;
    private List<GetNotificationInfoResDto> notificationList;
    private boolean hasNext;
    private String lastNotificationId;

    public static GetNotificationListResDto notPushEnabled() {
        return new GetNotificationListResDto(
                PushInfo.notPushEnabled(),
                List.of(),
                false,
                null
        );
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PushInfo {
        private boolean pushEnabled;
        private String message;

        public static PushInfo notPushEnabled() {
            return new PushInfo(
                    false,
                    NotificationSuccessCode.REQUEST_NOTIFICATION_PERMISSION.getMessage()
            );
        }

        public static PushInfo pushEnabled() {
            return new PushInfo(
                    true,
                    null
            );
        }
    }
}
