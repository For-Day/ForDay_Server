package com.example.ForDay.infra.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.example.ForDay.domain.app.type.ImageUsageType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3 amazonS3;
    private final S3Properties s3Properties;

    public String generateKey(ImageUsageType usage, String originalFilename) {
        // profile/temp/550e8400-e29b-41d4-a716-446655440000_mongsil.jpg
        String extension = extractExtension(originalFilename); // 활장자
        String baseName = originalFilename.substring(0, originalFilename.lastIndexOf(".")); // 이미지명
        return usage.name().toLowerCase()
                + "/temp/"
                + UUID.randomUUID()
                + "_" + baseName
                + extension;
    }

    // 파일 이름에서 확장자 추출 .jpg
    private String extractExtension(String filename) {
        int idx = filename.lastIndexOf(".");
        return idx != -1 ? filename.substring(idx) : "";
    }

    // s3에 presigned url을 만들어달라고 요청 (bucket 이름, key, contentType이 필요)
    // profile/temp/550e8400-e29b-41d4-a716-446655440000_mongsil.jpg
    /* https://my-bucket.s3.ap-northeast-2.amazonaws.com/profile/temp/550e8400-e29b-41d4-a716-446655440000_mongsil.jpg
    ?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=...&X-Amz-Date=...&X-Amz-Expires=600&X-Amz-Signature=...
    */
    public GeneratePresignedUrlRequest createPresignedPutRequest(
            String bucket,
            String key,
            String contentType
    ) {
        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(bucket, key)
                        .withMethod(HttpMethod.PUT)
                        .withExpiration(getExpiration()); // presignedUrl 유효시간 5분 설정

        request.addRequestParameter(
                Headers.CONTENT_TYPE,
                contentType
        );

        return request;
    }

    private Date getExpiration() {
        return new Date(System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5));
    }

    public String createFileUrl(String key) {
        return String.format(
                "https://%s.s3.%s.amazonaws.com/%s",
                s3Properties.getBucket(),
                s3Properties.getRegion(),
                key
        );
    }

    public boolean existsByKey(String key) {
        try {
            return amazonS3.doesObjectExist(
                    s3Properties.getBucket(),
                    key
            );
        } catch (Exception e) {
            throw new IllegalStateException("S3 파일 존재 여부 확인 실패: " + key, e);
        }
    }


    public void deleteByKey(String key) {
        try {
            amazonS3.deleteObject(
                    s3Properties.getBucket(),
                    key
            );
        } catch (Exception e) {
            throw new IllegalStateException("S3 파일 삭제 실패: " + key, e);
        }
    }

    public String extractKeyFromFileUrl(String fileUrl) {
        try {
            URI uri = URI.create(fileUrl);
            return uri.getPath().substring(1); // 앞의 "/" 제거
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 S3 파일 URL: " + fileUrl, e);
        }
    }
}
