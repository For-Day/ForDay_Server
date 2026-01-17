package com.example.ForDay.global.ai.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final WebClient webClient;

    @Value("${openai.api-key}")
    private String apiKey;

    public String call(String systemPrompt, String userPrompt) {

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", 0.7
        );

        return webClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2)) // 최대 3번 시도, 2초부터 시작해 대기 시간 증가
                        .filter(this::isRetryable) // 재시도할 에러 필터링
                        .doBeforeRetry(retrySignal ->
                                System.out.println("AI 요청 재시도 중... 사유: " + retrySignal.failure().getMessage()))
                )
                .block();
    }

    // 재시도 대상 에러 판단 (429 에러 및 네트워크 연결 끊김)
    private boolean isRetryable(Throwable throwable) {
        return throwable instanceof WebClientResponseException.TooManyRequests ||
                throwable instanceof java.io.IOException;
    }
}