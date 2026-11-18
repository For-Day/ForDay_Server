package com.example.ForDay.global.ai.service.moderation;

import com.example.ForDay.domain.record.type.ModerationCategory;
import com.example.ForDay.domain.record.type.ModerationVerdict;
import com.example.ForDay.global.ai.dto.moderation.ModerationJudgment;
import com.example.ForDay.global.ai.dto.moderation.ModerationLlmOutput;
import com.example.ForDay.global.util.PersonalInfoMasker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 이슈 #423 - LLM 출력을 검증하고 정책에 맞게 바로잡는다.
 *
 * <p>LLM 출력은 형식이 맞아도 내용이 모순될 수 있다(예: VIOLATION인데 category=NONE). 모순이나 알 수
 * 없는 값은 항상 "덜 위험한 쪽"인 UNCERTAIN(사람 검토)으로 보낸다 - 정상 기록을 잘못 가리는 것이
 * 이 기능의 가장 큰 피해이기 때문이다. 바로잡은 내역은 {@code normalizationNotes}로 남긴다.
 */
public final class ModerationResultNormalizer {

    private static final int MAX_EVIDENCE = 3;

    private ModerationResultNormalizer() {
    }

    public static ModerationJudgment normalize(ModerationLlmOutput out, Map<PersonalInfoMasker.Type, Integer> detected,
                                               String promptVersion) {
        if (out == null) {
            return failed("LLM 응답 없음", detected, promptVersion);
        }
        List<String> notes = new ArrayList<>();

        ModerationVerdict verdict = parse(ModerationVerdict.class, out.verdict());
        if (verdict == null) {
            notes.add("알 수 없는 verdict '" + out.verdict() + "' → UNCERTAIN");
            verdict = ModerationVerdict.UNCERTAIN;
        }
        ModerationCategory category = parse(ModerationCategory.class, out.category());
        if (category == null) {
            notes.add("알 수 없는 category '" + out.category() + "'");
            category = ModerationCategory.NONE;
            if (verdict == ModerationVerdict.VIOLATION) {
                notes.add("위반인데 카테고리를 알 수 없음 → UNCERTAIN");
                verdict = ModerationVerdict.UNCERTAIN;
            }
        }

        if (verdict == ModerationVerdict.VIOLATION && category == ModerationCategory.NONE) {
            notes.add("VIOLATION + NONE 모순 → UNCERTAIN");
            verdict = ModerationVerdict.UNCERTAIN;
        }
        if (verdict == ModerationVerdict.NO_VIOLATION && category != ModerationCategory.NONE) {
            notes.add("NO_VIOLATION인데 category=" + category + " → NONE");
            category = ModerationCategory.NONE;
        }

        double confidence = out.confidence() == null ? 0.0 : out.confidence();
        if (confidence < 0 || confidence > 1 || Double.isNaN(confidence)) {
            notes.add("confidence 범위 밖(" + out.confidence() + ") → 0");
            confidence = 0.0;
        }

        List<String> evidence = out.evidence() == null ? List.of()
                : out.evidence().stream().filter(e -> e != null && !e.isBlank()).limit(MAX_EVIDENCE).toList();

        return new ModerationJudgment(verdict, category, confidence,
                out.reasoning() == null ? "" : out.reasoning().strip(),
                evidence, detected, promptVersion, List.copyOf(notes));
    }

    public static ModerationJudgment failed(String reason, Map<PersonalInfoMasker.Type, Integer> detected,
                                            String promptVersion) {
        return new ModerationJudgment(ModerationVerdict.UNCERTAIN, ModerationCategory.NONE, 0.0,
                "판정 실패: " + reason, List.of(), detected, promptVersion, List.of("판정 실패 → UNCERTAIN"));
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
