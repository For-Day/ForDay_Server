package com.example.ForDay.global.measure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 2단계(@Async 비동기 분리) 측정을 위해 {@code @Async}를 켠다. {@code measure} 프로파일 전용이라
 * local/test/프로덕션(blue·green)에는 영향이 없다.
 *
 * <p>전용 실행자를 따로 정의하지 않는 것은 의도적이다. 2단계가 재현하려는 것은 "Spring 비동기를
 * 그냥 붙였을 때 무슨 일이 생기는가"이고, 그 기본값이 바로 Spring Boot의
 * {@code applicationTaskExecutor}다 — 코어 스레드 8개, <b>큐 용량 무제한</b>. 큐가 무제한이라
 * 요청은 즉시 반환되지만, 처리되지 못한 작업이 힙에 계속 쌓이고 프로세스가 죽으면 그대로 사라진다.
 * 이 실행자는 Micrometer가 자동 계측하므로 Grafana에서 큐 깊이를 그대로 볼 수 있다
 * ({@code executor_queued_tasks}).
 */
@Configuration
@Profile("measure")
@EnableAsync
public class MeasureAsyncConfig {
}
