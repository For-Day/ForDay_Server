package com.example.ForDay.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetUserScrapListResDto {
    private Long totalScrapCount;
    private Long lastScrapId;
    private List<ScrapDto> scrapList;
    private boolean hasNext;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScrapDto {
        private Long recordId;
        private String thumbnailImageUrl;
        private String sticker;
        private String memo;
        private LocalDateTime createdAt;
    }
}
