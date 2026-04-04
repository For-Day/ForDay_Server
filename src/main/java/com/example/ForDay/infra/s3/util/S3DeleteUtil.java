package com.example.ForDay.infra.s3.util;

import com.example.ForDay.infra.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.example.ForDay.global.common.constants.FileStorageConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3DeleteUtil {
    private final S3Service s3Service;
    private final S3Util s3Util;

    public void registerS3DeletionAfterCommit(String imageUrl) {
        if (!StringUtils.hasText(imageUrl)) return;

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                processS3Deletion(imageUrl);
            }
        });
    }


    private void processS3Deletion(String imageUrl) {
        try {
            String originalKey = s3Service.extractKeyFromFileUrl(imageUrl);
            List<String> keysToDelete = new ArrayList<>();
            keysToDelete.add(originalKey);

            // 경로 패턴에 따른 리사이징 이미지 키 추가
            if (originalKey.contains(ACTIVITY_RECORD)) {
                keysToDelete.add(s3Service.extractKeyFromFileUrl(s3Util.toFeedThumbResizedUrl(imageUrl)));
            } else if (originalKey.contains(PROFILE_IMAGE)) {
                keysToDelete.add(s3Service.extractKeyFromFileUrl(s3Util.toProfileMainResizedUrl(imageUrl)));
                keysToDelete.add(s3Service.extractKeyFromFileUrl(s3Util.toProfileListResizedUrl(imageUrl)));
            } else if (originalKey.contains(COVER_IMAGE)) {
                keysToDelete.add(s3Service.extractKeyFromFileUrl(s3Util.toCoverMainResizedUrl(imageUrl)));
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