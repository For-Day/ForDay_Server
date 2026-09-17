package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.domain.hobby.dto.request.SimpleActivityRecommendReqDto;
import com.example.ForDay.domain.hobby.dto.response.ActivityDto;
import com.example.ForDay.domain.hobby.dto.response.FastAPIRecommendResDto;
import com.example.ForDay.domain.hobby.dto.response.SimpleActivityRecommendResDto;
import com.example.ForDay.global.ai.service.AiSimpleActivityRecommendService;
import com.example.ForDay.global.common.error.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;

@ExtendWith(MockitoExtension.class)
@DisplayName("SimpleActivityRecommendService - 취미 생성 전 상태 없는 활동 추천")
class SimpleActivityRecommendServiceTest {

    @Mock private AiSimpleActivityRecommendService aiSimpleActivityRecommendService;
    @InjectMocks private SimpleActivityRecommendService sut;

    private final SimpleActivityRecommendReqDto reqDto =
            new SimpleActivityRecommendReqDto("독서", "휴식", 30);

    @Nested
    @DisplayName("AI가 활동을 정상적으로 반환하면")
    class WhenAiReturnsActivities {

        @Test
        @DisplayName("응답 DTO로 변환해 반환한다")
        void 응답을_변환해_반환한다() {
            given(aiSimpleActivityRecommendService.requestSimpleActivityRecommendAI(any()))
                    .willReturn(new FastAPIRecommendResDto(List.of(
                            ActivityDto.builder().topic("t1").content("c1").description("d1").build()
                    )));

            SimpleActivityRecommendResDto result = sut.recommend(reqDto);

            assertThat(result.getActivities()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("AI 응답이 비어 있으면")
    class WhenAiReturnsEmpty {

        @Test
        @DisplayName("AI_RESPONSE_INVALID 예외를 던진다")
        void 예외를_던진다() {
            given(aiSimpleActivityRecommendService.requestSimpleActivityRecommendAI(any()))
                    .willReturn(new FastAPIRecommendResDto(List.of()));

            assertThatThrownBy(() -> sut.recommend(reqDto))
                    .isInstanceOf(CustomException.class);
        }

        @Test
        @DisplayName("AI 응답이 null이어도 예외를 던진다")
        void null_응답도_예외를_던진다() {
            given(aiSimpleActivityRecommendService.requestSimpleActivityRecommendAI(any()))
                    .willReturn(null);

            assertThatThrownBy(() -> sut.recommend(reqDto))
                    .isInstanceOf(CustomException.class);
        }
    }
}
