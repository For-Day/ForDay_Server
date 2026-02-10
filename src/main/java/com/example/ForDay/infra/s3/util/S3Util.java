package com.example.ForDay.infra.s3.util;

import org.springframework.stereotype.Component;

@Component
public class S3Util {

    public String toProfileMainResizedUrl(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains("/temp/")) {
            return originalUrl;
        }
        return originalUrl.replace("/temp/", "/resized/main/");
    }

    public String toProfileListResizedUrl(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains("/temp/")) {
            return originalUrl;
        }
        return originalUrl.replace("/temp/", "/resized/list/");
    }

    public String toFeedThumbResizedUrl(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains("/temp/")) {
            return originalUrl;
        }
        return originalUrl.replace("/temp/", "/resized/thumb/");
    }

    public String toCoverMainResizedUrl(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains("/temp/")) {
            return originalUrl;
        }
        return originalUrl.replace("/temp/", "/resized/thumb/");
    }
}
