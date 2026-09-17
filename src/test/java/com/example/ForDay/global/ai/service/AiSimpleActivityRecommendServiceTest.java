package com.example.ForDay.global.ai.service;

import com.example.ForDay.domain.hobby.dto.request.SimpleActivityRecommendReqDto;
import com.example.ForDay.domain.hobby.dto.response.ActivityDto;
import com.example.ForDay.domain.hobby.dto.response.FastAPIRecommendResDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiSimpleActivityRecommendService - Spring AI 상태 없는 활동 추천")
class AiSimpleActivityRecommendServiceTest {

    @Mock private ChatClient chatClient;
    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec callResponseSpec;

    @Test
    @DisplayName("취미 정보만으로 AI 응답을 그대로 반환한다")
    void 응답을_그대로_반환한다() {
        AiSimpleActivityRecommendService sut = new AiSimpleActivityRecommendService(chatClient);

        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.options(any())).willReturn(requestSpec);
        given(requestSpec.user(anyString())).willReturn(requestSpec);
        given(requestSpec.call()).willReturn(callResponseSpec);

        FastAPIRecommendResDto response = new FastAPIRecommendResDto(List.of(
                ActivityDto.builder().topic("t1").content("c1").description("d1").build()
        ));
        given(callResponseSpec.entity(FastAPIRecommendResDto.class)).willReturn(response);

        FastAPIRecommendResDto result = sut.requestSimpleActivityRecommendAI(
                new SimpleActivityRecommendReqDto("독서", "휴식", 30));

        assertThat(result.getActivities()).hasSize(1);
    }
}
