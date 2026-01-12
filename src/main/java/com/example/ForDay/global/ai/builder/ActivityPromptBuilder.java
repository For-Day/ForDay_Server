package com.example.ForDay.global.ai.builder;

import com.example.ForDay.domain.hobby.dto.request.ActivityAIRecommendReqDto;
import com.example.ForDay.domain.hobby.dto.request.OthersActivityRecommendReqDto;
import org.springframework.stereotype.Component;

@Component
public class ActivityPromptBuilder {

    public String buildSystemPrompt() {
        return """
        너는 ‘포데이’라는 취미 습관 앱의 활동 추천 AI다.
        너의 목적은 사용자가 설정한 조건 안에서 '실패하지 않고 끝까지 갈 수 있는 활동'을 추천하는 것이다.

        중요 원칙:
        - 활동은 반드시 사용자가 선택한 취미 안에서만 제안한다.
        - 한 번 실행하는데 걸리는 시간은 사용자가 선택한 시간 이하여야 한다.
        - 취미 목적에 맞는 감정 상태를 고려해 활동을 설계한다.
        - 실행 횟수와 목표 기간을 고려해 '지치지 않는 강도'로 제안한다.
        
        절대 하지 말 것:
        - 이상적인 습관 제안
        - 동기부여, 성장, 목표 달성 강조
        - 하루 단위 강요
        - ‘루틴’, ‘습관’, ‘목표’, ‘달성’ 같은 단어 x
        """;
    }

    public String buildUserPrompt(ActivityAIRecommendReqDto req) {

        String durationText = Boolean.TRUE.equals(req.getIsDurationSet())
                ? "66일"
                : "기간 설정 없음";

        return String.format("""
        아래 사용자 조건을 모두 반영하여 추천 활동 카드 3개를 생성해줘.
        이 활동들은 단순히 '생각'하는 것이 아니라, 반드시 '몸을 움직이거나 도구를 쓰는 구체적인 행위'여야 해
        사용자가 "이거라면 지금 당장 할 수 있겠는데?"라고 느낄 만큼 구체적이고 만만한 액션이어야 해.
        이 활동들은 단순히 취미 주변을 맴도는 행위(예: 커피 마시기, 책상 치우기)가 아니라, **반드시 해당 취미의 핵심 동작이 포함된 최소 단위의 액션**이어야 해.

        [사용자 조건]
        - 취미: %s
        - 1회 실행 가능 시간: %d분 이하
        - 취미 목적: %s
        - 실행 빈도: 주 %d회
        - 목표 기간: %s일
        
        활동 설계 기준:
        1. topic (주제): 활동의 핵심 성격을 2~4단어로 요약해. (예: 문장 수집, 장비 점검, 환경 조성, 기록하기) 
        2. content (활동명): '동사'로 끝나는 구체적인 행동을 제시해. 단순히 '읽기'보다 '3문장 밑줄 긋기', '5페이지 읽고 접어두기'처럼 행동의 시작과 끝이 명확해야 해 또한 '생각하기', '느끼기' 같은 관념적 단어는 절대 금지. '꺼내기', '찍기', '닦기', '쓰기', '입기', '켜기' 등 행동이 눈에 보이는 동사를 사용해.
        4. 심리적 장벽 제거: 실패하기가 더 어려운 아주 작은 단위(Tiny Habit)로 제안해.
        5. '취미'를 잘하게 만드는 활동이 아니라 '취미'를 부담 없이 하게 만드는 활동'일 것
        6. 66번 반복 가능한 낮은 난이도
        7. 실패해도 부담이 없는 구조일 것
        8. 실행 난이도가 매우 낮을 것
        9. "오늘 한 번 해볼까?" 정도의 가벼움
        10. 도구 결합: 해당 취미와 관련된 물건(운동화, 펜, 프라이팬, 악보, 운동 앱 등)을 만지거나 준비하는 과정을 포함해.
        11. 마이크로 액션: 너무 쉬워서 실패하기가 더 어려운 아주 작은 단위의 '물리적 행동'으로 쪼개줘.
        
        출력 작성 규칙:
        - 명령형 표현 금지
        - 성과, 목표, 성장, 꾸준함 같은 단어 사용 금지
        - 설명은 안심시키는 톤으로 작성
        - 권장표현: ~하기, ~해보기, 횟수(5번, 14번)
        - 취미와 직접 관련 없는 부수적 행위 제안 금지 (예: 간식 먹기, 노래 듣기, 장소 이동하기)
        - 추상적인 상태 제안 금지 (예: 감상하기, 생각하기, 마음 다잡기)
        
        출력예시:
        - 책 5페이지만 읽기
        - 커피 마시며 에세이 읽기
        - 문단 1개만 소리내어 읽기
        - 출퇴근길 전자책 읽기
        - 내 기분이랑 비슷한 문장 찾기
       
        [출력 규칙]
        - 반드시 아래 JSON 형식만 출력할 것
        - JSON 외의 설명 문구는 절대 포함하지 말 것

        {
          "activities": [
            {
              "topic": "활동 주제 (15자 이내, 상황 중심)",
              "content": "활동명 (10자 내외, 부담 없는 표현)",
              "description": "활동 설명 (1문장, 왜 쉬운지만 설명)"
            }
          ]
        }
        """,
                req.getHobbyName(),
                req.getHobbyTimeMinutes(),
                String.join(", ", req.getHobbyPurposes()),
                req.getExecutionCount(),
                durationText
        );
    }

    public String buildOtherActivityUserPrompt(OthersActivityRecommendReqDto reqDto) {
        String durationText = Boolean.TRUE.equals(reqDto.getIsDurationSet()) ? "목표 기간 66일" : "기간 설정 없음";
        String purposesText = String.join(", ", reqDto.getHobbyPurposes());

        return String.format("""
        [맥락]
        우리 앱 ‘포데이’에서 아래 조건으로 '%s' 취미를 즐기는 다른 유저(포비)들이 실제 실천 중인 인기 활동 3개를 생성해줘.

        [사용자 조건]
        - 취미: %s
        - 시간: %d분 이내
        - 목적: %s
        - 빈도: 주 %d회
        - 설정: %s

        [활동 설계 지침]
        1. 반드시 해당 취미와 관련된 '물리적인 행위'여야 함 (생각하기 등 관념적 활동 금지).
        2. 다른 유저들이 앱에 직접 등록했을 법한 생생하고 구체적인 문장으로 작성할 것.
        3. %d분 이내에 완료 가능한 가벼운 활동일 것.

        [출력 규칙]
        - 아래 JSON 형식으로만 응답하고, JSON 외의 텍스트는 절대 포함하지 말 것.
        - id는 1부터 순차적으로 부여할 것.

        {
            "otherActivities": [
                  {
                    "id": 1,
                    "content": "실제 유저가 등록했을 법한 구체적인 활동 내용"
                   },
                    ...
            ]
        }
        """,
                reqDto.getHobbyName(),
                reqDto.getHobbyName(),
                reqDto.getHobbyTimeMinutes(),
                purposesText,
                reqDto.getExecutionCount(),
                durationText,
                reqDto.getHobbyTimeMinutes()
        );
    }

}


