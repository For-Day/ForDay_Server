package com.example.ForDay.domain.record.command;

/**
 * 기록 이미지 한 장의 값 묶음.
 */
public record RecordImageCommand(
        String imageUrl,
        Integer imageOrder,
        Long imageWidth,
        Long imageHeight
) {
}
