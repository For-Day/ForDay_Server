package com.example.ForDay.domain.hobby.dto;

import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StickerContext {
    private boolean durationSet;
    private boolean recordedToday;
    private int totalStickerNum;
    private int totalSlotCount;
    private int currentPage;
    private int totalPage;
    private int size;

    public static StickerContext of(Hobby hobby, boolean recordedToday, Integer page, int size) {
        int totalStickerNum = hobby.getCurrentStickerNum();
        int totalSlotCount = calculateTotalSlot(totalStickerNum, recordedToday);
        int totalPage = calculateTotalPage(totalSlotCount, size);
        int currentPage = resolvePage(page, totalSlotCount, size);

        validatePage(currentPage, totalPage);

        return StickerContext.builder()
                .durationSet(hobby.getGoalDays() != null)
                .recordedToday(recordedToday)
                .totalStickerNum(totalStickerNum)
                .totalSlotCount(totalSlotCount)
                .currentPage(currentPage)
                .totalPage(totalPage)
                .size(size)
                .build();
    }

    public boolean hasPrevious() {
        return currentPage > 1;
    }

    public boolean hasNext() {
        return currentPage < totalPage;
    }

    private static int calculateTotalSlot(int totalStickerNum, boolean recordedToday) {
        return recordedToday ? totalStickerNum : totalStickerNum + 1;
    }

    private static int calculateTotalPage(int totalSlotCount, int size) {
        return ((totalSlotCount - 1) / size) + 1;
    }

    private static int resolvePage(Integer page, int totalSlotCount, int size) {
        if (page != null) return page;
        return calculateCurrentPage(totalSlotCount, size);
    }

    private static void validatePage(int currentPage, int totalPage) {
        if (currentPage <= 0 || currentPage > totalPage) {
            throw new CustomException(ErrorCode.INVALID_PAGE_REQUEST);
        }
    }

    private static int calculateCurrentPage(int totalSlotCount, int size) {
        if (totalSlotCount <= 0) return 1;
        return ((totalSlotCount - 1) / size) + 1;
    }
}