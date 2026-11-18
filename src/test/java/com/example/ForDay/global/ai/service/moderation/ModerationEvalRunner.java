package com.example.ForDay.global.ai.service.moderation;

import com.example.ForDay.domain.record.type.ModerationVerdict;
import com.example.ForDay.global.ai.dto.moderation.ModerationJudgeInput;
import com.example.ForDay.global.ai.dto.moderation.ModerationJudgment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;

/**
 * 이슈 #423 - 신고 심사 LLM 오프라인 평가 러너. 기본 test에서 제외된다({@code moderation-eval} 태그).
 *
 * <pre>
 *   OPENAI_API_KEY=... ./gradlew moderationEval -Pruns=3 -Psplit=all -PpromptVersion=v1
 * </pre>
 *
 * <p>합성 평가셋만 쓴다(실제 사용자 데이터 미사용). 결과는 build/moderation-eval/ 아래에 JSON(판정 원문)과
 * Markdown(집계표)으로 남긴다. 키워드 채점이 아니라 판정·카테고리 일치로 채점한다.
 */
@Tag("moderation-eval")
class ModerationEvalRunner {

    private static final double AUTO_ACTION_MIN_CONFIDENCE = 0.8;

    record Row(String id, String split, List<String> tags, int run, List<String> expectedVerdicts,
               String expectedCategory, ModerationJudgment judgment, long millis) {

        boolean correct() {
            ModerationVerdict v = judgment.verdict();
            if (!expectedVerdicts.contains(v.name())) {
                return false;
            }
            return v != ModerationVerdict.VIOLATION || judgment.category().name().equals(expectedCategory);
        }

        boolean isTrueViolationCase() {
            return expectedVerdicts.contains("VIOLATION") && !"NONE".equals(expectedCategory);
        }

        boolean clearViolationCase() {
            return expectedVerdicts.equals(List.of("VIOLATION"));
        }
    }

    @Test
    @DisplayName("합성 평가셋으로 신고 심사 판정 정확도를 측정한다")
    void evaluate() throws Exception {
        String apiKey = System.getenv("OPENAI_API_KEY");
        Assumptions.assumeTrue(apiKey != null && apiKey.startsWith("sk-"), "OPENAI_API_KEY 미설정 - 평가 생략");

        int runs = Integer.parseInt(System.getProperty("moderation.eval.runs", "3"));
        String split = System.getProperty("moderation.eval.split", "all");
        String promptVersion = System.getProperty("moderation.eval.promptVersion", ModerationPromptBuilder.DEFAULT_VERSION);
        String model = System.getProperty("moderation.eval.model", "gpt-4o-mini");

        AiModerationJudgeService judge = new AiModerationJudgeService(chatClient(apiKey, model), promptVersion);
        List<JsonNode> cases = loadCases(split);

        ExecutorService pool = Executors.newFixedThreadPool(4);
        List<Future<Row>> futures = new ArrayList<>();
        for (JsonNode c : cases) {
            for (int run = 1; run <= runs; run++) {
                int r = run;
                futures.add(pool.submit(() -> evaluateOne(judge, c, r)));
            }
        }
        List<Row> rows = new ArrayList<>();
        for (Future<Row> f : futures) {
            rows.add(f.get());
        }
        pool.shutdown();

        String table = summarize(rows, promptVersion, model, runs);
        System.out.println(table);
        write(rows, table, promptVersion);
    }

    private static Row evaluateOne(AiModerationJudgeService judge, JsonNode c, int run) {
        List<String> reasons = new ArrayList<>();
        c.get("reasons").forEach(r -> reasons.add(r.asText()));
        List<String> tags = new ArrayList<>();
        c.get("tags").forEach(t -> tags.add(t.asText()));
        List<String> expected = new ArrayList<>();
        c.get("expected").get("verdicts").forEach(v -> expected.add(v.asText()));

        long started = System.currentTimeMillis();
        ModerationJudgment j = judge.judge(new ModerationJudgeInput(c.get("activity").asText(), c.get("memo").asText(), reasons));
        return new Row(c.get("id").asText(), c.get("split").asText(), tags, run, expected,
                c.get("expected").get("category").asText(), j, System.currentTimeMillis() - started);
    }

    static String summarize(List<Row> rows, String promptVersion, String model, int runs) {
        StringBuilder sb = new StringBuilder();
        sb.append("## 신고 심사 평가 - prompt ").append(promptVersion).append(", ").append(model)
                .append(", 사례당 ").append(runs).append("회\n\n");
        sb.append("| 구분 | 판정 적중 | 위반 정밀도 | 위반 재현율(명확) | 자동조치 정밀도(≥").append(AUTO_ACTION_MIN_CONFIDENCE)
                .append(") | 자동조치 건수 | UNCERTAIN 비율 | 인젝션 적중 | 신고남용 적중 | 평균 지연 |\n");
        sb.append("|---|---|---|---|---|---|---|---|---|---|\n");
        for (String split : List.of("all", "dev", "holdout")) {
            List<Row> rs = split.equals("all") ? rows : rows.stream().filter(r -> r.split().equals(split)).toList();
            if (rs.isEmpty()) {
                continue;
            }
            List<Row> predictedViolation = filter(rs, r -> r.judgment().verdict() == ModerationVerdict.VIOLATION);
            List<Row> clear = filter(rs, Row::clearViolationCase);
            List<Row> auto = filter(rs, r -> r.judgment().autoActionable(AUTO_ACTION_MIN_CONFIDENCE));
            sb.append("| ").append(split)
                    .append(" | ").append(ratio(filter(rs, Row::correct).size(), rs.size()))
                    .append(" | ").append(ratio(filter(predictedViolation, Row::isTrueViolationCase).size(), predictedViolation.size()))
                    .append(" | ").append(ratio(filter(clear, r -> r.judgment().verdict() == ModerationVerdict.VIOLATION).size(), clear.size()))
                    .append(" | ").append(ratio(filter(auto, r -> r.isTrueViolationCase()
                            && r.judgment().category().name().equals(r.expectedCategory())).size(), auto.size()))
                    .append(" | ").append(auto.size())
                    .append(" | ").append(ratio(filter(rs, r -> r.judgment().verdict() == ModerationVerdict.UNCERTAIN).size(), rs.size()))
                    .append(" | ").append(tagged(rs, "injection"))
                    .append(" | ").append(tagged(rs, "abuse"))
                    .append(" | ").append(rs.stream().mapToLong(Row::millis).average().orElse(0) / 1000.0).append("s |\n");
        }

        sb.append("\n### 틀린 판정\n\n| 사례 | split | 기대 | 판정(카테고리, 확신도) | 근거 |\n|---|---|---|---|---|\n");
        rows.stream().filter(r -> !r.correct()).forEach(r -> sb.append("| ").append(r.id()).append("#").append(r.run())
                .append(" | ").append(r.split())
                .append(" | ").append(String.join("/", r.expectedVerdicts())).append(" ").append(r.expectedCategory())
                .append(" | ").append(r.judgment().verdict()).append(" (").append(r.judgment().category()).append(", ")
                .append(r.judgment().confidence()).append(")")
                .append(" | ").append(r.judgment().reasoning().replace("|", "/").replace("\n", " ")).append(" |\n"));
        return sb.toString();
    }

    private static String tagged(List<Row> rs, String tag) {
        List<Row> t = filter(rs, r -> r.tags().contains(tag));
        return t.isEmpty() ? "-" : ratio(filter(t, Row::correct).size(), t.size());
    }

    private static List<Row> filter(List<Row> rs, Predicate<Row> p) {
        return rs.stream().filter(p).toList();
    }

    private static String ratio(int n, int d) {
        return d == 0 ? "-" : String.format("%d/%d (%.0f%%)", n, d, 100.0 * n / d);
    }

    private static ChatClient chatClient(String apiKey, String model) {
        OpenAiApi api = OpenAiApi.builder().apiKey(apiKey).build();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder().model(model).temperature(0.0).build())
                .build();
        return ChatClient.create(chatModel);
    }

    private static List<JsonNode> loadCases(String split) throws Exception {
        try (InputStream in = ModerationEvalRunner.class.getResourceAsStream("/moderation/eval-set.json")) {
            List<JsonNode> cases = new ArrayList<>();
            new ObjectMapper().readTree(in).get("cases").forEach(c -> {
                if (split.equals("all") || c.get("split").asText().equals(split)) {
                    cases.add(c);
                }
            });
            return cases;
        }
    }

    private static void write(List<Row> rows, String table, String promptVersion) throws Exception {
        Path dir = Path.of("build", "moderation-eval");
        Files.createDirectories(dir);
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + "-" + promptVersion;
        List<Map<String, Object>> raw = new ArrayList<>();
        for (Row r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.id());
            m.put("split", r.split());
            m.put("run", r.run());
            m.put("expected", r.expectedVerdicts() + " " + r.expectedCategory());
            m.put("correct", r.correct());
            m.put("judgment", r.judgment());
            m.put("millis", r.millis());
            raw.add(m);
        }
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(dir.resolve(stamp + ".json").toFile(), raw);
        Files.writeString(dir.resolve(stamp + ".md"), table);
        System.out.println("결과: " + dir.resolve(stamp).toAbsolutePath() + ".json / .md");
    }
}
