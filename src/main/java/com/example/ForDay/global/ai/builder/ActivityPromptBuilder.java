package com.example.ForDay.global.ai.builder;

import com.example.ForDay.domain.hobby.dto.request.ActivityAIRecommendReqDto;
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
        아래 사용자 조건을 모두 반영하여 추천 활동 카드 5개를 생성해줘.

        [사용자 조건]
        - 취미: %s
        - 1회 실행 가능 시간: %d분 이하
        - 취미 목적: %s
        - 실행 빈도: 주 %d회
        - 목표 기간: %s일
        
        활동 설계 기준:
        1. '취미'를 잘하게 만드는 활동이 아니라 '취미'를 부담 없이 하게 만드는 활동'일 것
        2. 66번 반복 가능한 낮은 난이도
        3. 실패해도 부담이 없는 구조일 것
        4. 실행 난이도가 매우 낮을 것
        5. "오늘 한 번 해볼까?" 정도의 가벼움
        
        출력 작성 규칙:
        - 명령형 표현 금지
        - 성과, 목표, 성장, 꾸준함 같은 단어 사용 금지
        - 설명은 안심시키는 톤으로 작성
        - 권장표현: ~하기, ~해보기, 횟수(5번, 14번)
        
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

}


