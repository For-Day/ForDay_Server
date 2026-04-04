package com.example.ForDay.global.common.constants;

public class FileStorageConstants {

    /**
     * S3 도메인별 메인 폴더 경로 (Root Folders)
     */
    public static final String ACTIVITY_RECORD = "activity_record";
    public static final String PROFILE_IMAGE = "profile_image";
    public static final String COVER_IMAGE = "cover_image";
    public static final String HOBBY_CARD = "hobby_card";

    public static final String TEST_PATH = "TEST_";

    /**
     * 임시 저장 및 특정 용도 경로 상수
     */
    public static final String TEMP_ACTIVITY_PATH = ACTIVITY_RECORD + "/temp/";
    public static final String TEMP_COVER_PATH = COVER_IMAGE + "/temp/";
    public static final String TEMP_HOBBY_CARD_PATH = HOBBY_CARD + "/temp/";

    /**
     * 경로 치환 및 리사이징 키워드
     */
    public static final String TEMP_DIR = "/temp/";
    public static final String THUMB_DIR = "/resized/thumb/";

    // 프로필 리사이즈 경로
    public static final String PROFILE_MAIN_DIR = "/resized/main/";
    public static final String PROFILE_LIST_DIR = "/resized/list/";

    // 기록 리사이즈 경로
    public static final String FEED_THUMB_DIR = "/resized/thumb/";

    // 커버 리사이즈 경로
    public static final String COVER_THUMB_DIR = "/resized/thumb/";

    private FileStorageConstants() {}
}