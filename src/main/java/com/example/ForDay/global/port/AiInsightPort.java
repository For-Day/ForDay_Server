package com.example.ForDay.global.port;

/**
 * 외부 AI 서버에 사용자 활동 요약을 요청한다.
 *
 * <p>여러 도메인이 공유하므로 global에 둔다 (ADR-0001 §7 포트 위치 규칙).
 */
public interface AiInsightPort {

    /**
     * @return 요약문. 호출 실패나 빈 응답이면 빈 문자열(기존 동작 유지).
     */
    String requestActivitySummary(String userId, Long hobbyId, String hobbyName);
}
