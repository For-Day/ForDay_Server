package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.domain.hobby.dto.StickerContext;
import com.example.ForDay.domain.hobby.entity.Hobby;
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

    public static GetStickerInfoResDto of(
            Hobby hobby,
            StickerContext ctx,
            List<StickerDto> stickers) {
        return new GetStickerInfoResDto(
                hobby.getId(),
                ctx.isDurationSet(),
                ctx.isRecordedToday(),
                ctx.getCurrentPage(),
                ctx.getTotalPage(),
                ctx.getSize(),
                ctx.getTotalStickerNum(),
                ctx.getCurrentPage() > 1,      // hasPrevious
                ctx.getCurrentPage() < ctx.getTotalPage(), // hasNext
                stickers
        );
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StickerDto {
        private Long activityRecordId;
        private String sticker;
        private boolean deleted;
    }
}
