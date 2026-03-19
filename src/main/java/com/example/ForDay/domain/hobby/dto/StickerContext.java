package com.example.ForDay.domain.hobby.dto;

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
}