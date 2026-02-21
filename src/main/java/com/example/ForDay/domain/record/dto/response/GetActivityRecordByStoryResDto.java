package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.hobby.dto.response.GetHobbyStoryTabsResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetActivityRecordByStoryResDto {
    private List<StoryTabInfo> tabInfo;
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
        private String memo;
        private UserInfoDto userInfo;
        private boolean pressedAweSome;
        private String hobbyName;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserInfoDto {
        private String userId;
        private String nickname;
        private String profileImageUrl;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StoryTabInfo {
        private Long hobbyId;
        private String hobbyName;
        private boolean currentHobby;

        public static StoryTabInfo from(Hobby hobby, boolean isCurrentHobby) {
            return new StoryTabInfo(
                    hobby.getId(),
                    hobby.getHobbyName(),
                    isCurrentHobby
            );
        }
    }
}
