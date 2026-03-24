package com.example.ForDay.infra.s3.util;

import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.infra.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static com.example.ForDay.global.common.constants.FileStorageConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Util {
    private final S3Service s3Service;

    public String toProfileMainResizedUrl(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains(TEMP_DIR)) {
            return originalUrl;
        }
        return originalUrl.replace(TEMP_DIR, PROFILE_MAIN_DIR);
    }

    public String toProfileListResizedUrl(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains(TEMP_DIR)) {
            return originalUrl;
        }
        return originalUrl.replace(TEMP_DIR, PROFILE_LIST_DIR);
    }

    public String toFeedThumbResizedUrl(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains(TEMP_DIR)) {
            return originalUrl;
        }
        return originalUrl.replace(TEMP_DIR, FEED_THUMB_DIR);
    }

    public String toCoverMainResizedUrl(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains(TEMP_DIR)) {
            return originalUrl;
        }
        return originalUrl.replace(TEMP_DIR, COVER_THUMB_DIR);
    }

    public void validateS3Image(String imageUrl) {
        if (StringUtils.hasText(imageUrl)) {
            String s3Key = s3Service.extractKeyFromFileUrl(imageUrl);
            if (!s3Service.existsByKey(s3Key)) {
                log.error("[RecordActivity] S3 이미지 부재 - Key: {}", s3Key);
                throw new CustomException(ErrorCode.S3_IMAGE_NOT_FOUND);
            }
        }
    }
}
