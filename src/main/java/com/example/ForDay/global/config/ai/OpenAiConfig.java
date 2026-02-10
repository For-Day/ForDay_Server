package com.example.ForDay.global.config.ai;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class OpenAiConfig {

    @Bean
    public WebClient openAiWebClient(@Value("${openai.base-url}") String baseUrl) {

        // 1. 커넥션 풀 설정: Idle 상태의 연결을 NAT 제한(350초)보다 훨씬 짧은 30초 후 폐기
        ConnectionProvider provider = ConnectionProvider.builder("openai-connection-pool")
                .maxConnections(50)
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(30))
                .build();

        // 2. HttpClient에 타임아웃 및 커넥션 풀 적용
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000) // 연결 시도 5초 제한
                .responseTimeout(Duration.ofSeconds(30));         // 응답 대기 30초 제한

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}