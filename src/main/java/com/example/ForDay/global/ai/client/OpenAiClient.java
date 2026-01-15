package com.example.ForDay.global.ai.client;

import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
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

        try {
            return webClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    // 🔴 429 명확히 분리
                    .onStatus(
                            HttpStatus.TOO_MANY_REQUESTS::equals,
                            response -> response.bodyToMono(String.class)
                                    .map(errorBody -> new CustomException(ErrorCode.AI_RATE_LIMIT_EXCEEDED))
                    )
                    .bodyToMono(String.class)
                    .delaySubscription(Duration.ofMillis(500))
                    .retryWhen(
                            Retry.fixedDelay(3, Duration.ofSeconds(1))
                                    .filter(ex -> ex instanceof CustomException &&
                                            ((CustomException) ex).getErrorCode() == ErrorCode.AI_RATE_LIMIT_EXCEEDED)
                    )
                    .block();

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            throw new CustomException(ErrorCode.AI_SERVICE_ERROR);
        }
    }
}
