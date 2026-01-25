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
    private int totalFeedCount;
    private Long lastRecordId;
    private List<FeedDto> feedList;
    private boolean hasNext;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedDto {
        private Long recordId;
        private String thumbnailImageUrl;
        private String sticker;
        private LocalDateTime createdAt;
    }
}
