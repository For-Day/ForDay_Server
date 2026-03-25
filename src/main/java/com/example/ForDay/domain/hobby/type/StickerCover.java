package com.example.ForDay.domain.hobby.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum StickerCover {
    SMILE("smile", "smile.png"),
    SAD("sad", "sad.png"),
    LAUGH("laugh", "laugh.png"),
    ANGRY("angry", "angry.png"); // 기본값 혹은 분기용

    private final String keyword;
    private final String fileName;

    private static final String BASE_URL = "https://forday-s3-bucket.s3.ap-northeast-2.amazonaws.com/default_cover/";

    public String getFullUrl() {
        return BASE_URL + this.fileName;
    }

    public static String getUrlBySticker(String sticker) {
        if (sticker == null) return ANGRY.getFullUrl();

        return Arrays.stream(values())
                .filter(cover -> sticker.contains(cover.keyword))
                .findFirst()
                .map(StickerCover::getFullUrl)
                .orElse(ANGRY.getFullUrl());
    }
}