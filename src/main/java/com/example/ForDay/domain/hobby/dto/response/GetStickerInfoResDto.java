package com.example.ForDay.domain.hobby.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GetStickerInfoResDto {
    private Long hobbyId;
    private boolean durationSet;
    private boolean activityRecordedToday;
    private Integer currentPage;
    private Integer totalPage;
    private Integer pageSize;
    private Integer totalStickerNum;
    private boolean hasPrevious;
    private boolean hasNext;
    private List<StickerDto> stickers;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StickerDto {
        private Long activityRecordId;
        private String sticker;
    }
}
