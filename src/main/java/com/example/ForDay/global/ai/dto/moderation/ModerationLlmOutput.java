package com.example.ForDay.global.ai.dto.moderation;

import java.util.List;

/**
 * 이슈 #423 - LLM 구조화 출력 형식. 값은 신뢰하지 않고 {@code ModerationResultNormalizer}가 검증·정규화한다.
 */
public record ModerationLlmOutput(String verdict, String category, Double confidence,
                                  String reasoning, List<String> evidence) {
}
