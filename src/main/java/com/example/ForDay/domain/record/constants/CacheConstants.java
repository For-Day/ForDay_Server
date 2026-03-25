package com.example.ForDay.domain.record.constants;

public class CacheConstants {
    public static final String STICKERS = "stickers";
    public static final String USER_FEED = "userFeed";

    public static final String STICKER_KEY_PATTERN = STICKERS + "::%d:%s:*"; // hobbyId(Long), userId(String)
    public static final String USER_FEED_KEY_PATTERN = USER_FEED + "::%s:*";  // userId(String)
}