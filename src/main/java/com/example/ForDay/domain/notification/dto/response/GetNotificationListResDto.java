package com.example.ForDay.domain.notification.dto.response;

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
                    "알림을 놓치지 않도록 알림 권한을 허용해주세요."
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
