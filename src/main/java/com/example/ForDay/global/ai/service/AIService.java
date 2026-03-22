package com.example.ForDay.global.ai.service;

import com.example.ForDay.domain.hobby.dto.request.FastAPIRecommendReqDto;
import com.example.ForDay.domain.hobby.dto.response.FastAPIRecommendResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.common.error.exception.CustomException;
import com.example.ForDay.global.common.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class AIService {
    private static final String AI_ACTIVITIES_RECOMMEND_PATH = "/ai/activities/recommend";

    @Value("${fastapi.url}")
    private String fastApiBaseUrl;
    private final RestTemplate restTemplate;

    public FastAPIRecommendResDto requestActivityRecommendAI(User user, Hobby hobby) {
        String url = fastApiBaseUrl + AI_ACTIVITIES_RECOMMEND_PATH;
        FastAPIRecommendReqDto requestDto = FastAPIRecommendReqDto.from(user, hobby);

        FastAPIRecommendResDto response = restTemplate.postForObject(url, requestDto, FastAPIRecommendResDto.class);

        if (response == null || response.getActivities().isEmpty()) {
            throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
        }
        return response;
    }
}
