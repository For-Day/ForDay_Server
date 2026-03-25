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

    public static GetUserScrapListResDto of(
            List<ScrapDto> scrapDtos, long totalCount, int size) {
        boolean hasNext = scrapDtos.size() > size;
        if (hasNext) scrapDtos.remove(size);

        Long lastId = scrapDtos.isEmpty()
                ? null
                : scrapDtos.get(scrapDtos.size() - 1).getScrapId();

        return new GetUserScrapListResDto(totalCount, lastId, scrapDtos, hasNext);
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ScrapDto {
        private Long scrapId;
        private Long recordId;
        private String thumbnailImageUrl;
        private String sticker;
        private String memo;
        private LocalDateTime createdAt;
    }
}
