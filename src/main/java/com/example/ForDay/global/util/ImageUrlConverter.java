package com.example.ForDay.global.util;

import org.springframework.stereotype.Component;

import static com.example.ForDay.global.common.constants.FileStorageConstants.*;

/**
 * 업로드 원본 URL을 용도별 리사이즈 경로 URL로 바꾼다.
 *
 * <p>외부 I/O가 없는 순수 문자열 변환이므로 포트를 두지 않는다.
 * (docs/architecture-rules.md §6 — 포트는 외부 자원 접근에만 둔다)
 */
@Component
public class ImageUrlConverter {

    public String toProfileMainResizedUrl(String originalUrl) {
        return replaceTempDir(originalUrl, PROFILE_MAIN_DIR);
    }

    public String toProfileListResizedUrl(String originalUrl) {
        return replaceTempDir(originalUrl, PROFILE_LIST_DIR);
    }

    public String toFeedThumbResizedUrl(String originalUrl) {
        return replaceTempDir(originalUrl, FEED_THUMB_DIR);
    }

    public String toCoverMainResizedUrl(String originalUrl) {
        return replaceTempDir(originalUrl, COVER_THUMB_DIR);
    }

    private String replaceTempDir(String originalUrl, String targetDir) {
        if (originalUrl == null || !originalUrl.contains(TEMP_DIR)) {
            return originalUrl;
        }
        return originalUrl.replace(TEMP_DIR, targetDir);
    }
}
