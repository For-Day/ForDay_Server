package com.example.ForDay.infra.s3.adapter;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.example.ForDay.domain.app.type.ImageUsageType;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.example.ForDay.global.port.ImageLifecyclePort;
import com.example.ForDay.global.port.ImageUploadPort;
import com.example.ForDay.global.port.ImageUrlPort;
import com.example.ForDay.global.port.UploadTarget;
import com.example.ForDay.global.util.ImageUrlConverter;
import com.example.ForDay.infra.s3.property.S3Properties;
import com.example.ForDay.infra.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.example.ForDay.global.common.constants.FileStorageConstants.ACTIVITY_RECORD;
import static com.example.ForDay.global.common.constants.FileStorageConstants.COVER_IMAGE;
import static com.example.ForDay.global.common.constants.FileStorageConstants.PROFILE_IMAGE;

/**
 * S3를 쓰는 이미지 저장소 어댑터.
 *
 * <p>세 포트를 함께 구현하지만, 호출하는 쪽은 필요한 포트에만 의존한다(ISP).
 * AWS SDK 타입은 이 클래스 밖으로 나가지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3ImageStorageAdapter implements ImageUrlPort, ImageUploadPort, ImageLifecyclePort {

    private final S3Service s3Service;
    private final S3Properties s3Properties;
    private final AmazonS3 amazonS3;
    private final ImageUrlConverter imageUrlConverter;

    @Override
    public String createFileUrl(String key) {
        return s3Service.createFileUrl(key);
    }

    @Override
    public String extractKeyFromFileUrl(String fileUrl) {
        return s3Service.extractKeyFromFileUrl(fileUrl);
    }

    @Override
    public UploadTarget issueUploadUrl(ImageUsageType usage, String originalFilename, String contentType) {
        String key = s3Service.generateKey(usage, originalFilename);

        GeneratePresignedUrlRequest request =
                s3Service.createPresignedPutRequest(s3Properties.getBucket(), key, contentType);

        String uploadUrl = amazonS3.generatePresignedUrl(request).toString();
        return new UploadTarget(uploadUrl, s3Service.createFileUrl(key));
    }

    @Override
    public void validateExists(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) {
            return;
        }
        String key = s3Service.extractKeyFromFileUrl(imageUrl);
        if (!s3Service.existsByKey(key)) {
            log.error("[S3] 이미지 부재 - Key: {}", key);
            throw new CustomException(ErrorCode.S3_IMAGE_NOT_FOUND);
        }
    }

    @Override
    public void copy(String sourceKey, String destinationKey) {
        s3Service.copyObject(sourceKey, destinationKey);
    }

    @Override
    public void deleteAfterCommit(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                delete(imageUrl);
            }
        });
    }

    private void delete(String imageUrl) {
        try {
            String originalKey = s3Service.extractKeyFromFileUrl(imageUrl);
            List<String> keysToDelete = new ArrayList<>();
            keysToDelete.add(originalKey);

            // 경로 패턴에 따른 리사이징 이미지 키 추가
            if (originalKey.contains(ACTIVITY_RECORD)) {
                keysToDelete.add(s3Service.extractKeyFromFileUrl(imageUrlConverter.toFeedThumbResizedUrl(imageUrl)));
            } else if (originalKey.contains(PROFILE_IMAGE)) {
                keysToDelete.add(s3Service.extractKeyFromFileUrl(imageUrlConverter.toProfileMainResizedUrl(imageUrl)));
                keysToDelete.add(s3Service.extractKeyFromFileUrl(imageUrlConverter.toProfileListResizedUrl(imageUrl)));
            } else if (originalKey.contains(COVER_IMAGE)) {
                keysToDelete.add(s3Service.extractKeyFromFileUrl(imageUrlConverter.toCoverMainResizedUrl(imageUrl)));
            }

            keysToDelete.forEach(key -> {
                if (StringUtils.hasText(key)) {
                    s3Service.deleteByKey(key);
                    log.info("[S3-Cleanup] 삭제 완료: {}", key);
                }
            });

        } catch (Exception e) {
            log.error("[S3-Cleanup] 삭제 중 오류 발생 (URL: {}): {}", imageUrl, e.getMessage());
        }
    }
}
