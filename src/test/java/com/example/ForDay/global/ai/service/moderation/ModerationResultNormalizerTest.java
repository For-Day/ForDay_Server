package com.example.ForDay.global.ai.service.moderation;

import com.example.ForDay.domain.record.type.ModerationCategory;
import com.example.ForDay.domain.record.type.ModerationVerdict;
import com.example.ForDay.global.ai.dto.moderation.ModerationJudgment;
import com.example.ForDay.global.ai.dto.moderation.ModerationLlmOutput;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ModerationResultNormalizerTest {

    private static ModerationJudgment normalize(String verdict, String category, Double confidence) {
        return ModerationResultNormalizer.normalize(
                new ModerationLlmOutput(verdict, category, confidence, " 근거 ", List.of("a", "", "b", "c", "d")),
                Map.of(), "v1");
    }

    @Test
    @DisplayName("정상 출력은 그대로 채택하고 근거는 최대 3개로 자른다")
    void accepted() {
        ModerationJudgment j = normalize("VIOLATION", "SPAM_AD", 0.92);

        assertThat(j.verdict()).isEqualTo(ModerationVerdict.VIOLATION);
        assertThat(j.category()).isEqualTo(ModerationCategory.SPAM_AD);
        assertThat(j.evidence()).containsExactly("a", "b", "c");
        assertThat(j.reasoning()).isEqualTo("근거");
        assertThat(j.normalizationNotes()).isEmpty();
    }

    @Nested
    @DisplayName("모순·알 수 없는 값은 사람 검토(UNCERTAIN) 쪽으로 보낸다")
    class TowardsHumanReview {

        @Test
        @DisplayName("VIOLATION인데 category가 NONE이면 UNCERTAIN")
        void violationWithoutCategory() {
            ModerationJudgment j = normalize("VIOLATION", "NONE", 0.9);

            assertThat(j.verdict()).isEqualTo(ModerationVerdict.UNCERTAIN);
            assertThat(j.normalizationNotes()).anyMatch(n -> n.contains("모순"));
        }

        @Test
        @DisplayName("알 수 없는 verdict·category는 UNCERTAIN")
        void unknownValues() {
            assertThat(normalize("MAYBE", "SPAM_AD", 0.5).verdict()).isEqualTo(ModerationVerdict.UNCERTAIN);
            assertThat(normalize("VIOLATION", "DRUGS", 0.9).verdict()).isEqualTo(ModerationVerdict.UNCERTAIN);
        }

        @Test
        @DisplayName("NO_VIOLATION인데 카테고리가 붙어 있으면 카테고리를 NONE으로 바로잡는다")
        void noViolationWithCategory() {
            ModerationJudgment j = normalize("no_violation", "SEXUAL", 0.8);

            assertThat(j.verdict()).isEqualTo(ModerationVerdict.NO_VIOLATION);
            assertThat(j.category()).isEqualTo(ModerationCategory.NONE);
        }

        @Test
        @DisplayName("응답이 없거나 confidence가 범위 밖이면 자동 조치 대상이 될 수 없다")
        void failedOrOutOfRange() {
            assertThat(ModerationResultNormalizer.normalize(null, Map.of(), "v1").verdict())
                    .isEqualTo(ModerationVerdict.UNCERTAIN);
            assertThat(normalize("VIOLATION", "SPAM_AD", 1.7).autoActionable(0.8)).isFalse();
        }
    }

    @Test
    @DisplayName("자동 조치는 허용 카테고리 + 기준 이상 확신도인 위반만")
    void autoActionable() {
        assertThat(normalize("VIOLATION", "SPAM_AD", 0.9).autoActionable(0.8)).isTrue();
        assertThat(normalize("VIOLATION", "SPAM_AD", 0.7).autoActionable(0.8)).isFalse();
        assertThat(normalize("VIOLATION", "HATE_HARASSMENT", 0.99).autoActionable(0.8)).isFalse();
    }
}
