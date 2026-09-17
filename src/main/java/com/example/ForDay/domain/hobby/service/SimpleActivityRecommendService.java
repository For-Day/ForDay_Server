package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.domain.hobby.dto.request.SimpleActivityRecommendReqDto;
import com.example.ForDay.domain.hobby.dto.response.FastAPIRecommendResDto;
import com.example.ForDay.domain.hobby.dto.response.SimpleActivityRecommendResDto;
import com.example.ForDay.global.ai.service.AiSimpleActivityRecommendService;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 이슈 #395 - 취미를 만들기 전, 취미 정보만으로 활동을 미리 추천받는 상태 없는
 * (stateless) 엔드포인트. 과거 기록 조회, AI 호출 횟수 제한이 없다는 점에서
 * {@link com.example.ForDay.domain.hobby.service.v1.HobbyService#activityAiRecommend}
 * 와 다르다.
 */
@Service
@RequiredArgsConstructor
public class SimpleActivityRecommendService {

    private final AiSimpleActivityRecommendService aiSimpleActivityRecommendService;

    public SimpleActivityRecommendResDto recommend(SimpleActivityRecommendReqDto reqDto) {
        FastAPIRecommendResDto response = aiSimpleActivityRecommendService.requestSimpleActivityRecommendAI(reqDto);

        if (response == null || response.getActivities() == null || response.getActivities().isEmpty()) {
            throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
        }
        return SimpleActivityRecommendResDto.of(response.getActivities());
    }
}
