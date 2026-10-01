package com.example.ForDay.global.measure.controller;

import com.example.ForDay.global.measure.dto.ResizeSyncReqDto;
import com.example.ForDay.global.measure.service.SyncImageResizeMeasurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code measure} 프로파일 전용. "기록 생성 요청 안에서 서버가 동기로 리사이즈했다면"을
 * 재현하는 엔드포인트. 이름이 {@code Test}로 시작해 ArchUnit S3(컨트롤러는 Docs 인터페이스를
 * 구현한다) 대상에서 제외된다.
 */
@RestController
@Profile("measure")
@RequiredArgsConstructor
public class TestImageResizeMeasurementController {
    private final SyncImageResizeMeasurementService syncImageResizeMeasurementService;

    @PostMapping("/measure/image/resize-sync")
    public void resizeSync(@RequestBody ResizeSyncReqDto reqDto) {
        syncImageResizeMeasurementService.resizeSync(reqDto.imageKeys());
    }
}
