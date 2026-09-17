package com.example.ForDay.global.discord.dto;

/**
 * Discord 웹훅이 받는 최소 페이로드. {@code content}만 채우면 텍스트 메시지로 전송된다.
 */
public record DiscordWebhookReqDto(String content) {
}
