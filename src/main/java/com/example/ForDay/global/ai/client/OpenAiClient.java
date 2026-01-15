package com.example.ForDay.global.ai.client;

import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private static final long MIN_INTERVAL_MS = 2000; // ⭐ 2초 throttle (필수)
    private static long lastCallTime = 0;

    private final WebClient webClient;

    @Value("${openai.api-key}")
    private String apiKey;

    /** 서버 단위 throttle */
    private synchronized void throttle() {
        long now = System.currentTimeMillis();
        long wait = MIN_INTERVAL_MS - (now - lastCallTime);
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallTime = System.currentTimeMillis();
    }

    public String call(String systemPrompt, String userPrompt) {

        throttle(); // ⭐⭐⭐ 핵심

        log.info("[OPENAI-CALL] time={}, thread={}",
                System.currentTimeMillis(),
                Thread.currentThread().getName()
        );

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.7
        );

        try {
            return webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .onStatus(
                            HttpStatus.TOO_MANY_REQUESTS::equals,
                            response -> response.bodyToMono(String.class)
                                    .map(bodyStr ->
                                            new CustomException(ErrorCode.AI_RATE_LIMIT_EXCEEDED))
                    )
                    .bodyToMono(String.class)
                    // 🔽 retry는 최소로 (429에서는 재시도 안 함)
                    .retryWhen(
                            Retry.fixedDelay(1, Duration.ofSeconds(1))
                                    .filter(ex ->
                                            !(ex instanceof CustomException &&
                                                    ((CustomException) ex).getErrorCode()
                                                            == ErrorCode.AI_RATE_LIMIT_EXCEEDED))
                    )
                    .block();

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.AI_SERVICE_ERROR);
        }
    }
}
