package com.example.ForDay.global.ai.service;

import com.example.ForDay.domain.activity.dto.request.FastAPIHobbyCardReqDto;
import com.example.ForDay.domain.activity.dto.response.FastAPIHobbyCardResDto;
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
public class AiHobbyCardService {
    private static final String AI_HOBBY_CARD_CONTENT_PATH = "/ai/hobby-card/content";

    @Value("${fastapi.url}")
    private String fastApiBaseUrl;
    private final RestTemplate restTemplate;

    public FastAPIHobbyCardResDto requestHobbyCardContentAI(Hobby hobby) {
        String url = fastApiBaseUrl + AI_HOBBY_CARD_CONTENT_PATH;
        FastAPIHobbyCardReqDto requestDto = FastAPIHobbyCardReqDto.builder()
                .userHobbyId(hobby.getId())
                .build();

        FastAPIHobbyCardResDto response = restTemplate.postForObject(url, requestDto, FastAPIHobbyCardResDto.class);

        if (response == null || response.getContent().isEmpty()) {
            throw new CustomException(ErrorCode.AI_RESPONSE_INVALID);
        }
        return response;
    }
}
