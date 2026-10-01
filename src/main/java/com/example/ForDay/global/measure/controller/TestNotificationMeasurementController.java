package com.example.ForDay.global.measure.controller;

import com.example.ForDay.global.measure.service.MeasurementStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 알림 파이프라인 측정 결과 조회·초기화 엔드포인트. {@code measure} 프로파일 전용.
 *
 * <p>부하 테스트 중·후에 브라우저로 바로 열어 수치를 확인하고 화면째 캡처하기 위한 것이다.
 * 카운터가 Redis에 있어서 앱 프로세스를 강제 종료해도 살아남는다 — 그래서 재시작 뒤에 이
 * 엔드포인트를 열면 "죽기 전까지 몇 건이 실제로 나갔는가"를 그대로 볼 수 있다.
 *
 * <p>이름이 {@code Test}로 시작하는 이유는 ArchUnit S3(컨트롤러는 Docs 인터페이스를 구현한다)
 * 대상에서 제외되기 위해서다 — 측정용 컨트롤러는 Swagger 문서화 대상이 아니다.
 */
@RestController
@Profile("measure")
@RequiredArgsConstructor
@RequestMapping("/measure/notification")
public class TestNotificationMeasurementController {

    private final MeasurementStatsService measurementStatsService;

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return measurementStatsService.stats();
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        return measurementStatsService.reset();
    }
}
