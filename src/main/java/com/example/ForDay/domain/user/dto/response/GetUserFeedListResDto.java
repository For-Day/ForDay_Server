package com.example.ForDay.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetUserFeedListResDto {
    private Long totalFeedCount;
    private Long lastRecordId;
    private List<FeedDto> feedList;
    private boolean hasNext;

    public static GetUserFeedListResDto of(
            List<FeedDto> feedList, Long totalFeedCount, int feedSize) {
        boolean hasNext = feedList.size() > feedSize;
        if (hasNext) feedList.remove(feedSize);

        Long lastId = feedList.isEmpty()
                ? null
                : feedList.get(feedList.size() - 1).getRecordId();

        return new GetUserFeedListResDto(totalFeedCount, lastId, feedList, hasNext);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedDto {
        private Long recordId;
        private String thumbnailImageUrl;
        private String sticker;
        private String memo;
        private LocalDateTime createdAt;
    }
}
