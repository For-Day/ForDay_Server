package com.example.ForDay.global.ai.adapter;

import com.example.ForDay.domain.hobby.dto.request.ActivitySummaryRequest;
import com.example.ForDay.domain.hobby.dto.response.ActivitySummaryResponse;
import com.example.ForDay.global.port.AiInsightPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * FastAPI 요약 엔드포인트 호출만 담당한다.
 *
 * <p>캐시 정책과 "요약을 만들지 말지" 판단은 도메인(HobbyAiSummaryService)에 있다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiAiInsightAdapter implements AiInsightPort {

    public static final String AI_SUMMARY_PATH = "/ai/summary";

    private final RestTemplate restTemplate;

    @Value("${fastapi.url}")
    private String fastApiBaseUrl;

    @Override
    public String requestActivitySummary(String userId, Long hobbyId, String hobbyName) {
        try {
            ActivitySummaryRequest requestDto = ActivitySummaryRequest.of(userId, hobbyId, hobbyName);

            ActivitySummaryResponse response = restTemplate.postForObject(
                    fastApiBaseUrl + AI_SUMMARY_PATH, requestDto, ActivitySummaryResponse.class);

            if (response != null && response.getSummary() != null) {
                return response.getSummary();
            }
        } catch (Exception e) {
            log.error("FastAPI 요약 요청 실패 | userId: {}, hobbyId: {}, error: {}", userId, hobbyId, e.getMessage());
        }
        return "";
    }
}
