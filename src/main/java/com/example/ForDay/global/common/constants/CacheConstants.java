package com.example.ForDay.global.common.constants;

public class CacheConstants {
    public static final String STICKERS = "stickers";
    public static final String USER_FEED = "userFeed";
    public static final String HOBBY_ACTIVITIES = "hobbyActivities";
    public static final String FRIEND_RELATIONS = "friendRelations";
    public static final String STICKER_KEY_PATTERN = STICKERS + "::%d:%s:*"; // hobbyId(Long), userId(String)
    public static final String USER_FEED_KEY_PATTERN = USER_FEED + "::%s:*";  // userId(String)
    public static final String HOBBY_ACTIVITIES_KEY_PATTERN = HOBBY_ACTIVITIES + "::%s:%d:*";
    public static final String FRIEND_RELATIONS_KEY_PATTERN = FRIEND_RELATIONS + "::*%s*%s*";
}