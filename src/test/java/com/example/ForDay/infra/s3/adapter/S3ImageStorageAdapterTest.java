package com.example.ForDay.infra.s3.adapter;

import com.amazonaws.services.s3.AmazonS3;
import com.example.ForDay.global.util.ImageUrlConverter;
import com.example.ForDay.infra.s3.property.S3Properties;
import com.example.ForDay.infra.s3.service.S3Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3ImageStorageAdapterTest {

    @Mock
    private S3Service s3Service;

    @Mock
    private S3Properties s3Properties;

    @Mock
    private AmazonS3 amazonS3;

    @Mock
    private ImageUrlConverter imageUrlConverter;

    @InjectMocks
    private S3ImageStorageAdapter adapter;

    @Test
    @DisplayName("키가 비어있으면 삭제를 시도하지 않는다")
    void deleteOrphanCopy_doesNothing_whenKeyBlank() {
        adapter.deleteOrphanCopy("");
        adapter.deleteOrphanCopy(null);

        verify(s3Service, never()).deleteByKey(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("키가 있으면 즉시 삭제한다 (트랜잭션 커밋을 기다리지 않음)")
    void deleteOrphanCopy_deletesImmediately_whenKeyPresent() {
        adapter.deleteOrphanCopy("cover_image/temp/abc.jpg");

        verify(s3Service).deleteByKey("cover_image/temp/abc.jpg");
    }

    @Test
    @DisplayName("삭제 중 예외가 나도 호출부로 전파하지 않는다 - 보상 삭제 실패가 원래 실패의 후처리를 막으면 안 된다")
    void deleteOrphanCopy_swallowsException() {
        willThrow(new RuntimeException("S3 unavailable"))
                .given(s3Service).deleteByKey("cover_image/temp/abc.jpg");

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(
                () -> adapter.deleteOrphanCopy("cover_image/temp/abc.jpg"));
    }
}
