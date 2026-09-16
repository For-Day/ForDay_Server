package com.example.ForDay.global.discord.adapter;

import com.example.ForDay.global.discord.dto.DiscordWebhookReqDto;
import com.example.ForDay.global.port.DiscordAlertPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Discord 웹훅으로 운영 알림을 보낸다. 기존 {@code RestTemplateConfig}의 공유 빈을 그대로 쓴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DiscordWebhookAdapter implements DiscordAlertPort {

    private final RestTemplate restTemplate;

    // 아직 설정 전이면 빈 문자열 - 기능이 없어도 알림 발행 자체는 막히면 안 된다.
    @Value("${discord.webhook.notification-url:}")
    private String webhookUrl;

    @Override
    public void send(String message) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("[discord] webhookUrl 미설정, 알림 스킵 - message: {}", message);
            return;
        }
        try {
            restTemplate.postForObject(webhookUrl, new DiscordWebhookReqDto(message), String.class);
        } catch (Exception e) {
            log.warn("[discord] 웹훅 전송 실패 - error: {}", e.getMessage());
        }
    }
}
