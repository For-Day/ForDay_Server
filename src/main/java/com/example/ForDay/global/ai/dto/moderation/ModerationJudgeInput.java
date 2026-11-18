package com.example.ForDay.global.ai.dto.moderation;

import java.util.List;

/**
 * 이슈 #423 - 신고 1차 심사 입력. 기록 1건과 그 기록에 들어온 신고 사유들(기록 단위 판정).
 *
 * <p>메모와 신고 사유는 사용자 입력이라 신뢰하지 않는다 - 프롬프트 빌더가 마스킹하고 데이터 블록으로 감싼다.
 */
public record ModerationJudgeInput(String activityName, String memo, List<String> reportReasons) {

    public ModerationJudgeInput {
        reportReasons = reportReasons == null ? List.of() : List.copyOf(reportReasons);
    }
}
