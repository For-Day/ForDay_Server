package com.example.ForDay.global.ai.adapter;

import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.global.ai.document.AiCallLog;
import com.example.ForDay.global.ai.service.AiCallLogService;
import com.example.ForDay.global.ai.type.AiCallType;
import com.example.ForDay.global.port.AiInsightPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 이슈 #387 - Spring AI ChatClient로 활동 요약을 생성한다.
 *
 * <p>캐시 정책과 "요약을 만들지 말지" 판단은 여전히 도메인(HobbyAiSummaryService)에 있다.
 * 이 클래스는 최근 7일 활동 기록 조회와 AI 호출만 담당한다. 클래스 이름은
 * FastAPI 호출 시절 그대로 유지했다(AiInsightPort 구현체라는 역할은 동일하고,
 * 호출부는 포트 타입으로만 의존하므로 이름 변경이 필요하지 않다).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiAiInsightAdapter implements AiInsightPort {

    private static final int RECENT_DAYS = 7;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("a hh시 mm분", Locale.KOREAN);

    private static final PromptTemplate USER_SUMMARY_PROMPT = new PromptTemplate("""
            당신은 사용자의 취미 활동 기록을 짧고 구체적인 한 문장으로 요약하는 전문가입니다.

            [입력 데이터]
            취미: {hobbyName}
            활동 기록:
            {pastActivities}

            [목적]
            - 일주일 동안의 취미 활동을 한 문장으로 요약하여 사용자가 자신의 활동을 가볍게 돌아볼 수 있도록 한다.

            [요약 기준]
            - 일주일간 지속한 **활동명**과 **활동 시간대**를 중심으로 구성
            - 평가나 성취가 아닌, 관찰 기반의 서술만 사용

            [작성 규칙]
            1. 15자 이내로 작성하세요.
            2. 반드시 "~을 하셨네요!" 형태로 문장을 종료하세요.
            3. 시간대, 장소, 대상, 활동 특징 중 하나를 포함하여 구체적으로 작성하세요.
            4. 평가나 성취가 아닌, 관찰 기반의 서술만 사용하세요.
            5. 이모지 사용 금지.
            6. 부연 설명 없이 딱 '한 문장'만 출력하세요.

            [출력 예시]
            - 주로 아침시간에 독서활동을 하셨네요!
            - 주로 풍경사진을 찍으셨네요!
            - 회사 점심을 위해 요리활동을 하셨네요!

            요약 결과:""");

    private final ChatClient chatClient;
    private final ActivityRecordRepository activityRecordRepository;
    private final AiCallLogService aiCallLogService;

    @Value("${ai.model}")
    private String model;

    @Value("${ai.temperature}")
    private double temperature;

    @Override
    public String requestActivitySummary(String userId, Long hobbyId, String hobbyName) {
        Map<String, Object> promptVariables = null;
        try {
            List<ActivityRecord> recentRecords = activityRecordRepository.findRecentByUserIdAndHobbyId(
                    userId, hobbyId, LocalDateTime.now().minusDays(RECENT_DAYS));

            promptVariables = Map.of(
                    "hobbyName", hobbyName,
                    "pastActivities", formatRecords(recentRecords)
            );
            String prompt = USER_SUMMARY_PROMPT.render(promptVariables);

            String summary = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            boolean success = StringUtils.hasText(summary);
            aiCallLogService.record(AiCallLog.builder()
                    .userId(userId)
                    .hobbyId(hobbyId)
                    .callType(AiCallType.USER_SUMMARY)
                    .model(model)
                    .temperature(temperature)
                    .prompt(promptVariables)
                    .rawResponse(summary)
                    .success(success)
                    .errorCode(success ? null : "EMPTY_RESPONSE")
                    .build());

            return success ? summary.strip() : "";
        } catch (Exception e) {
            log.error("AI 요약 요청 실패 | userId: {}, hobbyId: {}, error: {}", userId, hobbyId, e.getMessage());
            aiCallLogService.record(AiCallLog.builder()
                    .userId(userId)
                    .hobbyId(hobbyId)
                    .callType(AiCallType.USER_SUMMARY)
                    .model(model)
                    .temperature(temperature)
                    .prompt(promptVariables)
                    .success(false)
                    .errorCode(e.getClass().getSimpleName())
                    .build());
        }
        return "";
    }

    private String formatRecords(List<ActivityRecord> records) {
        return records.stream()
                .map(r -> "- " + r.getCreatedAt().format(TIME_FORMATTER) + ": " + r.getMemo())
                .collect(Collectors.joining("\n"));
    }
}
