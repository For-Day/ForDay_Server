package com.example.ForDay.global.ai.dto.moderation;

import com.example.ForDay.domain.record.type.ModerationCategory;
import com.example.ForDay.domain.record.type.ModerationVerdict;
import com.example.ForDay.global.util.PersonalInfoMasker;

import java.util.List;
import java.util.Map;

/**
 * 이슈 #423 - 정규화를 거친 최종 판정.
 *
 * @param normalizationNotes 코드가 LLM 출력을 바로잡은 내역(평가·감사용). 비어 있으면 그대로 채택.
 */
public record ModerationJudgment(
        ModerationVerdict verdict,
        ModerationCategory category,
        double confidence,
        String reasoning,
        List<String> evidence,
        Map<PersonalInfoMasker.Type, Integer> personalInfoDetected,
        String promptVersion,
        List<String> normalizationNotes
) {

    /**
     * 사람 검토 없이 자동으로 가려도 되는 판정인지. 정책상 자동 조치가 허용된 카테고리이면서
     * 확신도가 기준 이상인 위반만 해당한다(기준값은 평가셋의 정밀도로 정한다).
     */
    public boolean autoActionable(double minConfidence) {
        return verdict == ModerationVerdict.VIOLATION && category.autoActionAllowed() && confidence >= minConfidence;
    }
}
