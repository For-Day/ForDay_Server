package com.example.ForDay.global.common.constants;

public class CacheConstants {
    public static final String STICKERS = "stickers";
    public static final String USER_FEED = "userFeed";
    public static final String HOBBY_ACTIVITIES = "hobbyActivities";
    public static final String FRIEND_RELATIONS = "friendRelations";
    public static final String ACTIVITY_RECORD = "activityRecord";
    public static final String REACTION_LOCK = "reaction:lock";
    public static final String REACTION_QUEUE = "reaction_queue";
    public static final String STICKER_KEY_PATTERN = STICKERS + "::%d:%s:*";
    public static final String USER_FEED_KEY_PATTERN = USER_FEED + "::%s:*";
    public static final String HOBBY_ACTIVITIES_KEY_PATTERN = HOBBY_ACTIVITIES + "::%s:%d:*";
    public static final String FRIEND_RELATIONS_KEY_PATTERN = FRIEND_RELATIONS + "::*%s*%s*";
    public static final String RECORD_KEY_PATTERN = ACTIVITY_RECORD + "::%d";
    public static final String REACTION_LOCK_KEY = REACTION_LOCK + ":%d:%s:%s";
    public static final String REACTION_QUEUE_FORMAT = "%s:%d:%s";
}