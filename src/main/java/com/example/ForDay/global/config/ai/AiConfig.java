package com.example.ForDay.global.config.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 이슈 #385 - FastAPI(LangChain) 체인을 대체할 Spring AI ChatClient 빈 구성.
 *
 * <p>기존 FastAPI 체인({@code activity_chain.py})이 쓰던 모델/temperature 값을 그대로
 * 기본값으로 맞춘다({@code ai.model}, {@code ai.temperature}).
 */
@Configuration
public class AiConfig {

    @Value("${ai.model}")
    private String model;

    @Value("${ai.temperature}")
    private double temperature;

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .build())
                .build();
    }
}
