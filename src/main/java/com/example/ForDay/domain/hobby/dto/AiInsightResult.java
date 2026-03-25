package com.example.ForDay.domain.hobby.dto;

public record AiInsightResult(
        String summaryText,
        boolean isCallRemaining,
        int remainingCallCount
) {
    public static AiInsightResult unavailable() {
        return new AiInsightResult("", false, 0);
    }
}