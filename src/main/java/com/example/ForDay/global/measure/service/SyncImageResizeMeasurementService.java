package com.example.ForDay.global.measure.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.example.ForDay.infra.s3.property.S3Properties;
import lombok.RequiredArgsConstructor;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.example.ForDay.global.common.constants.FileStorageConstants.FEED_THUMB_DIR;
import static com.example.ForDay.global.common.constants.FileStorageConstants.TEMP_DIR;

/**
 * "기록 생성 요청 안에서 서버가 직접 리사이즈했다면" 가상 시나리오를 재현/측정하기 위한
 * {@code measure} 프로파일 전용 서비스. 실제 프로덕션은 이 코드를 전혀 쓰지 않는다 -
 * S3 ObjectCreated 이벤트로 Lambda(fordayImageResizeFunction)가 비동기로 리사이즈한다.
 *
 * <p>기록 저장(DB) 로직은 의도적으로 빼고 S3 다운로드→리사이즈→S3 업로드만 격리했다 -
 * 이 세 단계가 정확히 "서버에서 동기로 리사이즈했다면 요청 스레드가 무엇을 기다리는가"에
 * 해당하는 변수이고, DB 쓰기 비용은 Lambda든 서버든 동일하게 발생해 노이즈만 된다.
 *
 * <p>대상 키는 반드시 {@code test_activity_record/temp/}(TEST_ACTIVITY_RECORD usage) 하위여야
 * 한다 - 실제 {@code activity_record/temp/}에 올리면 프로덕션 Lambda가 같은 객체를 보고
 * 실제로 리사이즈를 또 한 번 수행해(콘솔에서 관리되는 S3 이벤트 트리거라 이 저장소 코드로는
 * 범위를 확인할 수 없다), 측정 부하가 실제 Lambda 호출/CloudWatch 지표를 오염시킨다.
 */
@Service
@Profile("measure")
@RequiredArgsConstructor
public class SyncImageResizeMeasurementService {

    private static final int THUMB_WIDTH = 400;

    private final AmazonS3 amazonS3;
    private final S3Properties s3Properties;

    public void resizeSync(List<String> imageKeys) {
        for (String key : imageKeys) {
            byte[] original = download(key);
            byte[] resized = resize(original);
            upload(toResizedKey(key), resized);
        }
    }

    private byte[] download(String key) {
        try (S3Object object = amazonS3.getObject(s3Properties.getBucket(), key);
             InputStream content = object.getObjectContent()) {
            return content.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("S3 다운로드 실패: " + key, e);
        }
    }

    private byte[] resize(byte[] original) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Thumbnails.of(new ByteArrayInputStream(original))
                    .width(THUMB_WIDTH)
                    .outputFormat("jpg")
                    .toOutputStream(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("리사이즈 실패", e);
        }
    }

    private void upload(String key, byte[] data) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(data.length);
        metadata.setContentType("image/jpeg");
        amazonS3.putObject(s3Properties.getBucket(), key, new ByteArrayInputStream(data), metadata);
    }

    private String toResizedKey(String originalKey) {
        return originalKey.replace(TEMP_DIR, FEED_THUMB_DIR);
    }
}
