package com.example.ForDay.domain.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class StickerWithActivityIdDto {
    private Long activityId;
    private Long activityRecordId;
    private String sticker;
}
