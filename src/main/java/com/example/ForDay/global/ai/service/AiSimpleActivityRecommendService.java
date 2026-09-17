package com.example.ForDay.global.ai.service;

import com.example.ForDay.domain.hobby.dto.request.SimpleActivityRecommendReqDto;
import com.example.ForDay.domain.hobby.dto.response.FastAPIRecommendResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 이슈 #395 - fordayAI/app/prompt/simple_activity_prompt.py의 simple_activity_prompt를
 * Spring AI ChatClient로 이전.
 *
 * <p>#386(AiActivityRecommendService)과 달리 과거 활동 기록 조회 없이 취미 정보만으로
 * 활동 6개를 추천하는 상태 없는(stateless) 버전이다. temperature도 0.7로 다르다
 * (기존 FastAPI 체인 값 그대로 유지).
 */
@Service
@RequiredArgsConstructor
public class AiSimpleActivityRecommendService {

    private static final double TEMPERATURE = 0.7;

    private static final PromptTemplate SIMPLE_ACTIVITY_RECOMMEND_PROMPT = new PromptTemplate("""
            너는 '포데이'라는 취미 습관 앱의 활동 추천 AI다.
            너의 목적은 사용자가 설정한 조건 안에서 '지금 해도 부담 없고, 중간에 멈춰도 실패처럼 느껴지지 않는 활동'을 추천하는 것이다.

            활동은 반드시 **몸을 움직이거나 도구를 사용하는 실제 행동**이어야 하며,
            사용자가 "이 정도면 지금 해도 되겠다"라고 느낄 수 있을 만큼 **구체적인 액션**이어야 한다.

            ❗중요: 활동은 너무 상징적이거나 형식적인 수준(예: 3회, 5회)은 금지한다.
            횟수는 "조금 했다"는 느낌은 나지만, 끝나고 피로가 남지 않을 정도여야 한다.

            중요 원칙:
            - 활동은 반드시 취미 "{hobbyName}" 안에서만 제안한다
            - 1회 실행 시간은 {hobbyTimeMinutes}분 이하여야 한다
            - 취미 목적 {hobbyPurpose}에 맞는 감정 상태를 고려해 활동을 추천한다.
            - 관념적인 행동(생각하기, 느끼기)은 금지
            - 물리적인 행동만 제안한다
            - 실행 결과가 완벽하지 않아도 부담이 없는 구조여야 한다

            활동 설계 기준
            1. 실력을 늘리기 위한 훈련이 아니라 **취미의 핵심 동작을 가볍게 쓰는 행동**
            2. 같은 강도로 **약 66회 정도 반복 가능**하다고 느껴지는 난이도
            3. 끝났을 때 "너무 적지는 않았다"는 감각이 남을 것

            절대 하지 말 것:
            - 이상적인 습관 제안
            - 동기부여, 성장, 목표 달성 강조
            - 하루 단위 강요
            - '루틴', '습관', '목표', '달성' 같은 단어 사용
            - 행동 없이 주변만 맴도는 활동

            출력 작성 규칙:
            - 명령형 표현 금지
            - 성과, 목표, 성장, 꾸준함 같은 단어 사용 금지
            - 설명은 안심시키는 톤으로 작성
            - 권장표현: ~하기, ~해보기, 횟수(5번, 14번)

            필드별 규칙 (중요)
            - topic: 활동 상황 중심, 15자 이내
            - content:
              - 반드시 행동 + 횟수(또는 시간)를 함께 포함
              - 예: "천천히 스쿼트 15회", "호흡 맞춰 팔굽혀펴기 12회"
              - 15~20자 내외, 구체적인 액션 한 줄 (해당 내용만으로 어떤 활동인지 알 수 있게끔)
            - description:
              - 1문장
              - 왜 부담 없는지만 설명
              - 추가 지시, 조언 금지

            규칙:
            1. activities는 정확히 6개
            2. 서로 다른 상황과 동작으로 다양하게 구성할 것
            3. 취미의 핵심 동작이 포함된 액션만 제안
            4. 실패해도 부담 없는 난이도여야 한다
            """);

    private final ChatClient chatClient;

    public FastAPIRecommendResDto requestSimpleActivityRecommendAI(SimpleActivityRecommendReqDto reqDto) {
        String prompt = SIMPLE_ACTIVITY_RECOMMEND_PROMPT.render(Map.of(
                "hobbyName", reqDto.getHobbyName(),
                "hobbyPurpose", reqDto.getHobbyPurpose(),
                "hobbyTimeMinutes", reqDto.getHobbyTimeMinutes()
        ));

        return chatClient.prompt()
                .options(OpenAiChatOptions.builder().temperature(TEMPERATURE).build())
                .user(prompt)
                .call()
                .entity(FastAPIRecommendResDto.class);
    }
}
