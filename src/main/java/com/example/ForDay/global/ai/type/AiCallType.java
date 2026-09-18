package com.example.ForDay.global.ai.type;

/**
 * 이슈 #407 - {@code ai_call_logs} 컬렉션에 남기는 AI 호출 종류.
 *
 * <p>호출 지점(Ai*Service/Adapter)과 1:1로 대응한다.
 */
public enum AiCallType {
    HOBBY_CARD,
    ACTIVITY_RECOMMEND,
    SIMPLE_RECOMMEND,
    USER_SUMMARY
}
