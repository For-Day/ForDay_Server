package com.example.ForDay.domain.record.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetActivityRecordByStoryResDto {
    private Long hobbyId;
    private Long lastRecordId;
    private List<RecordDto> recordList;
    private boolean hasNext;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RecordDto {
        private Long recordId;
        private String thumbnailUrl;
        private String sticker;
        private String title;
        private UserInfoDto userInfo;
        private boolean pressedAweSome;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfoDto {
        private String userId;
        private String nickname;
        private String profileImageUrl;
    }
}
