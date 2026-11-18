package com.example.ForDay.domain.record.type;

/**
 * 이슈 #423 - 신고 1차 심사 판정.
 *
 * <p>{@link #UNCERTAIN}은 "사람이 검토해야 함"이다. 판정 실패(LLM 오류·형식 오류)도 여기로 보낸다 -
 * 실패가 자동 조치로 이어지지 않게 하는 것이 최우선이다.
 */
public enum ModerationVerdict {
    VIOLATION,
    NO_VIOLATION,
    UNCERTAIN
}
