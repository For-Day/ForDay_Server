package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.hobby.dto.response.GetHobbyStoryTabsResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.global.util.ImageUrlConverter;
import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetActivityRecordByStoryResDto {
    private boolean unReadNotificationExists;
    private List<StoryTabInfo> tabInfo;
    private Long lastRecordId;
    private List<RecordDto> recordList;
    private boolean hasNext;

    public static GetActivityRecordByStoryResDto of(
            boolean unReadNotificationExists,
            List<StoryTabInfo> tabInfos,
            List<RecordDto> recordDtos,
            int size) {
        boolean hasNext = recordDtos.size() > size;
        if (hasNext) recordDtos.remove(size);

        Long lastId = recordDtos.isEmpty()
                ? null
                : recordDtos.get(recordDtos.size() - 1).getRecordId();

        return new GetActivityRecordByStoryResDto(unReadNotificationExists, tabInfos, lastId, recordDtos, hasNext);
    }


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
        private boolean recordAuthor;

        public void convertImageUrls(ImageUrlConverter imageUrlConverter) {
            if (this.thumbnailUrl != null) {
                this.thumbnailUrl = imageUrlConverter.toFeedThumbResizedUrl(this.thumbnailUrl);
            }
            if (this.userInfo != null && this.userInfo.getProfileImageUrl() != null) {
                this.userInfo.setProfileImageUrl(
                        imageUrlConverter.toProfileListResizedUrl(this.userInfo.getProfileImageUrl())
                );
            }
        }
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
