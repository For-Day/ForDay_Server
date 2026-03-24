package com.example.ForDay.global.common.constants;

public class AiMessageConstants {
    public static final String HOBBY_RECOMMENDATION_SUFFIX = " 포데이 AI가 알맞은 취미 활동을 추천드려요";
    public static final String DEFAULT_HOBBY_SUMMARY = "포데이 AI가 알맞은 취미 활동을 추천드려요";
    public static final String PREVIOUS_RECOMMENDATION_SUFFIX = " 이전에 추천 받은 활동들이에요.";
    public static final String DEFAULT_PREVIOUS_RECOMMENDATION = "이전에 추천 받은 활동들이에요.";

    /**
     * AI 요약 결과와 공통 문구를 조합합니다.
     */
    public static String formatHobbySummary(String summary) {
        if (summary == null || summary.isBlank()) {
            return DEFAULT_HOBBY_SUMMARY;
        }
        return summary + HOBBY_RECOMMENDATION_SUFFIX;
    }

    public static String formatPreviousRecommendation(String summary) {
        if (summary == null || summary.isBlank()) {
            return DEFAULT_PREVIOUS_RECOMMENDATION;
        }
        return summary + PREVIOUS_RECOMMENDATION_SUFFIX;
    }
}