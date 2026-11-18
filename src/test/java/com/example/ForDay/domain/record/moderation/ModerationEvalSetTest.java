package com.example.ForDay.domain.record.moderation;

import com.example.ForDay.domain.record.type.ModerationCategory;
import com.example.ForDay.global.util.PersonalInfoMasker;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이슈 #421 - 신고 심사 합성 평가셋(src/test/resources/moderation/eval-set.json)의 형식을 지킨다.
 *
 * <p>평가셋은 프롬프트 개선 효과를 재는 기준이라, 잘못 편집되면 이후 측정이 전부 틀어진다.
 */
class ModerationEvalSetTest {

    private static final Set<String> VERDICTS = Set.of("VIOLATION", "NO_VIOLATION", "UNCERTAIN");
    private static List<JsonNode> cases;

    @BeforeAll
    static void load() throws IOException {
        try (InputStream in = ModerationEvalSetTest.class.getResourceAsStream("/moderation/eval-set.json")) {
            JsonNode root = new ObjectMapper().readTree(in);
            cases = new ArrayList<>();
            root.get("cases").forEach(cases::add);
        }
    }

    @Test
    @DisplayName("id가 겹치지 않고, 판정·카테고리 값이 정책과 일치한다")
    void valuesMatchPolicy() {
        Set<String> ids = new HashSet<>();
        Set<String> categories = new HashSet<>();
        Arrays.stream(ModerationCategory.values()).forEach(c -> categories.add(c.name()));

        for (JsonNode c : cases) {
            assertThat(ids.add(c.get("id").asText())).as("중복 id %s", c.get("id")).isTrue();
            assertThat(categories).contains(c.get("expected").get("category").asText());
            c.get("expected").get("verdicts").forEach(v -> assertThat(VERDICTS).contains(v.asText()));
            assertThat(c.get("split").asText()).isIn("dev", "holdout");
        }
    }

    @Test
    @DisplayName("privacy 태그가 붙은 사례는 마스커가 개인정보를 1건 이상 탐지한다")
    void privacyCasesAreDetectedByMasker() {
        List<JsonNode> privacy = cases.stream()
                .filter(c -> c.get("tags").toString().contains("\"privacy\""))
                .toList();

        assertThat(privacy).isNotEmpty();
        for (JsonNode c : privacy) {
            PersonalInfoMasker.Result result = PersonalInfoMasker.mask(c.get("memo").asText());
            assertThat(result.detected()).as("%s: %s", c.get("id"), c.get("memo")).isTrue();
        }
    }

    @Test
    @DisplayName("홀드아웃이 있고, 위반·정상 사례가 모두 들어 있다")
    void hasHoldoutAndBothClasses() {
        assertThat(cases).anyMatch(c -> c.get("split").asText().equals("holdout"));
        assertThat(cases).anyMatch(c -> c.get("expected").get("category").asText().equals("NONE"));
        assertThat(cases).anyMatch(c -> !c.get("expected").get("category").asText().equals("NONE"));
    }
}
