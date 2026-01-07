package com.example.ForDay.global.ai.service;

import com.example.ForDay.domain.hobby.dto.request.ActivityAIRecommendReqDto;
import com.example.ForDay.global.ai.builder.ActivityPromptBuilder;
import com.example.ForDay.global.ai.client.OpenAiClient;
import com.example.ForDay.global.ai.dto.response.AiActivityResult;
import com.example.ForDay.global.ai.dto.response.OpenAiRawResponse;
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

    public AiActivityResult recommend(ActivityAIRecommendReqDto reqDto) throws Exception {

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(reqDto);

        String rawResponse = openAiClient.call(systemPrompt, userPrompt);
        //log.info("[AI RAW RESPONSE] {}", rawResponse);

        OpenAiRawResponse responseContainer = objectMapper.readValue(rawResponse, OpenAiRawResponse.class);
        String contentJson = responseContainer.getChoices().get(0).getMessage().getContent();

        // 3. content 내부의 JSON 문자열을 최종 DTO로 변환 (마크다운 제거 포함)
        return objectMapper.readValue(
                cleanJsonMarkdown(contentJson),
                AiActivityResult.class
        );
    }

    private String cleanJsonMarkdown(String content) {
        if (content == null) return "{}";
        return content.replaceAll("```json|```", "").trim();
    }
}
