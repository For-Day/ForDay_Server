package com.example.ForDay.global.port;

import com.example.ForDay.domain.app.type.ImageUsageType;

/**
 * 클라이언트가 직접 업로드할 수 있는 URL 발급.
 *
 * <p>presigned URL 발급에 필요한 AWS 타입은 어댑터 안에 가둔다.
 */
public interface ImageUploadPort {

    UploadTarget issueUploadUrl(ImageUsageType usage, String originalFilename, String contentType);
}
