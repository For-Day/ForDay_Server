package com.example.ForDay.global.ai.service;

import com.example.ForDay.domain.activity.dto.response.FastAPIHobbyCardResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.record.dto.HobbyCardActivityStatDto;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 이슈 #388 - 취미별 활동 통계 기반 카드 문구 생성을 Spring AI ChatClient로 직접 호출한다.
 *
 * <p>Hobby/Activity/ActivityRecord 조인 + TOP5 활동·시간대 집계는 기존 FastAPI의
 * SQLAlchemy 쿼리를 {@link ActivityRecordRepository}의 QueryDSL 쿼리로 포팅했다.
 */
@Service
@RequiredArgsConstructor
public class AiHobbyCardService {

    private static final int TOP_ACTIVITY_LIMIT = 5;
    private static final String DEFAULT_CONTENT = "꾸준히 쌓아온 기록들이 멋진 결실을 맺었네요!";

    private static final PromptTemplate HOBBY_CARD_PROMPT = new PromptTemplate("""
            당신은 취미 활동을 한 줄로 요약하는 전문가입니다.

            [데이터]
            취미: {hobbyName}
            가장 자주 한 활동 TOP 5: {activities}
            주요 활동 시간대: {timePattern}

            [작성 규칙]
            1. 15자 이내로 작성
            2. "~활동" 형태로 끝내기
            3. 시간대, 장소, 함께 하는 사람, 특징 중 하나를 포함
            4. 구체적이고 개성 있게
            5. 이모지 사용 금지

            [좋은 예시]
            - 주로 아침시간을 활용한 독서활동
            - 회사 점심을 책임진 요리활동
            - 주로 풍경화를 찍었던 사진촬영
            - 주로 친구와 함께하는 카페투어
            - 퇴근 후 혼자 즐기는 러닝활동
            - 주말 아침을 여는 베이킹활동

            [나쁜 예시]
            - 독서를 좋아하는 사람 (X - 너무 일반적)
            - 매일매일 열심히 독서활동 (X - 추상적)
            - 아침에 책 읽기 (X - "~활동" 형태 아님)
            - 🌅 새벽 독서 생활 (X - 이모지 사용)

            한 문장만 출력하세요. 설명 없이 문장만 작성하세요.

            요약 결과:""");

    private final ChatClient chatClient;
    private final ActivityRecordRepository activityRecordRepository;

    public FastAPIHobbyCardResDto requestHobbyCardContentAI(Hobby hobby) {
        List<HobbyCardActivityStatDto> stats =
                activityRecordRepository.findTopActivityStatsByHobbyId(hobby.getId(), TOP_ACTIVITY_LIMIT);

        if (stats.isEmpty()) {
            return new FastAPIHobbyCardResDto(DEFAULT_CONTENT);
        }

        String prompt = HOBBY_CARD_PROMPT.render(Map.of(
                "hobbyName", hobby.getHobbyName(),
                "activities", topActivitiesText(stats),
                "timePattern", resolveTimePattern(stats)
        ));

        String content = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        if (!StringUtils.hasText(content)) {
            throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
        }
        return new FastAPIHobbyCardResDto(content.strip());
    }

    private String topActivitiesText(List<HobbyCardActivityStatDto> stats) {
        return stats.stream()
                .map(HobbyCardActivityStatDto::getContent)
                .collect(Collectors.joining(", "));
    }

    private String resolveTimePattern(List<HobbyCardActivityStatDto> stats) {
        List<Long> activityIds = stats.stream().map(HobbyCardActivityStatDto::getActivityId).toList();
        List<Integer> hours = activityRecordRepository.findRecordHoursByActivityIds(activityIds);

        double avgHour = hours.isEmpty()
                ? 12
                : hours.stream().mapToInt(Integer::intValue).average().orElse(12);

        if (avgHour < 6) return "새벽";
        if (avgHour < 12) return "아침";
        if (avgHour < 18) return "오후";
        return "밤";
    }
}
