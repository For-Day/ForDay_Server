package com.example.ForDay.global.ai.service;

import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.global.ai.builder.ActivityPromptBuilder;
import com.example.ForDay.global.ai.client.OpenAiClient;
import com.example.ForDay.global.ai.dto.response.AiActivityResult;
import com.example.ForDay.global.ai.dto.response.AiOthersActivityResult;
import com.example.ForDay.global.ai.dto.response.OpenAiRawResponse;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiActivityService {

    private final OpenAiClient openAiClient;
    private final ActivityPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public AiActivityResult activityRecommend(Hobby hobby) {
        try {
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(hobby);

            // 1. AI 호출
            String rawResponse = openAiClient.call(systemPrompt, userPrompt);

            // 2. OpenAI 전체 응답 객체 파싱
            OpenAiRawResponse responseContainer = objectMapper.readValue(rawResponse, OpenAiRawResponse.class);

            // 3. choices 혹은 content가 비어있는지 검증
            if (responseContainer.getChoices() == null || responseContainer.getChoices().isEmpty()) {
                log.error("[AI_RESPONSE_INVALID] Response choices are empty");
                throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
            }

            String contentJson = responseContainer.getChoices().get(0).getMessage().getContent();

            // 4. 추출된 JSON 문자열을 최종 DTO로 변환
            return objectMapper.readValue(
                    cleanJsonMarkdown(contentJson),
                    AiActivityResult.class
            );

        } catch (JsonProcessingException e) {
            // JSON 파싱 중 오류 발생 (Jackson 관련 예외)
            log.error("[AI_RESPONSE_INVALID] JSON 파싱 에러: {}", e.getMessage());
            throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
        } catch (RuntimeException e) {
            // 네트워크 타임아웃, OpenAI 서버 오류 등
            log.error("[AI_SERVICE_ERROR] AI API 호출 또는 런타임 에러: {}", e.getMessage());
            throw new CustomException(ErrorCode.AI_SERVICE_ERROR);
        } catch (Exception e) {
            // 그 외 모든 체크드 예외 처리
            log.error("[INTERNAL_SERVER_ERROR] 예외 발생: {}", e.getMessage());
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public AiOthersActivityResult othersActivityRecommend(Hobby hobby) {
        try {
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildOtherActivityUserPrompt(hobby);

            String rawResponse = openAiClient.call(systemPrompt, userPrompt);

            OpenAiRawResponse responseContainer = objectMapper.readValue(rawResponse, OpenAiRawResponse.class);

            if (responseContainer.getChoices() == null || responseContainer.getChoices().isEmpty()) {
                throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
            }

            String contentJson = responseContainer.getChoices().get(0).getMessage().getContent();

            // AI가 응답한 JSON을 AiOthersActivityResult로 바로 매핑
            return objectMapper.readValue(
                    cleanJsonMarkdown(contentJson),
                    AiOthersActivityResult.class
            );

        } catch (JsonProcessingException e) {
            log.error("[AI_RESPONSE_INVALID] JSON 파싱 에러: {}", e.getMessage());
            throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
        } catch (Exception e) {
            log.error("[AI_SERVICE_ERROR] AI 호출 중 에러: {}", e.getMessage());
            throw new CustomException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    private String cleanJsonMarkdown(String content) {
        if (content == null) return "{}";
        return content.replaceAll("```json|```", "").trim();
    }
}
