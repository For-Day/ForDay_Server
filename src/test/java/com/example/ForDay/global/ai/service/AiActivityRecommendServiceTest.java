package com.example.ForDay.global.ai.service;

import com.example.ForDay.domain.hobby.dto.response.ActivityDto;
import com.example.ForDay.domain.hobby.dto.response.FastAPIRecommendResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.ai.document.AiCallLog;
import com.example.ForDay.global.common.error.exception.CustomException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AiActivityRecommendService - Spring AI 활동 추천")
class AiActivityRecommendServiceTest {

    private static final Long HOBBY_ID = 10L;
    private static final String USER_ID = "user-1";

    @Mock private ChatClient chatClient;
    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec callResponseSpec;
    @Mock private ActivityRecordRepository activityRecordRepository;
    @Mock private AiCallLogService aiCallLogService;
    @Mock private User user;
    @Mock private Hobby hobby;

    private AiActivityRecommendService sut;

    private void givenHobbyAndChatClient() {
        sut = new AiActivityRecommendService(chatClient, activityRecordRepository, aiCallLogService);

        given(user.getId()).willReturn(USER_ID);
        given(hobby.getId()).willReturn(HOBBY_ID);
        given(hobby.getHobbyName()).willReturn("독서");
        given(hobby.getHobbyPurpose()).willReturn("휴식");
        given(hobby.getHobbyTimeMinutes()).willReturn(30);
        given(hobby.getExecutionCount()).willReturn(3);
        given(hobby.getGoalDays()).willReturn(66);

        given(activityRecordRepository.findMemosByUserIdAndHobbyId(anyString(), anyLong()))
                .willReturn(List.of());

        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.user(anyString())).willReturn(requestSpec);
        given(requestSpec.call()).willReturn(callResponseSpec);
    }

    @Nested
    @DisplayName("AI가 3개의 활동을 정상적으로 반환하면")
    class WhenAiReturnsActivities {

        @Test
        @DisplayName("그대로 응답한다")
        void 응답을_그대로_반환한다() {
            givenHobbyAndChatClient();
            FastAPIRecommendResDto response = new FastAPIRecommendResDto(List.of(
                    ActivityDto.builder().topic("t1").content("c1").description("d1").build()
            ));
            given(callResponseSpec.entity(FastAPIRecommendResDto.class)).willReturn(response);

            FastAPIRecommendResDto result = sut.requestActivityRecommendAI(user, hobby);

            assertThat(result.getActivities()).hasSize(1);

            ArgumentCaptor<AiCallLog> captor = ArgumentCaptor.forClass(AiCallLog.class);
            verify(aiCallLogService).record(captor.capture());
            assertThat(captor.getValue().isSuccess()).isTrue();
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getHobbyId()).isEqualTo(HOBBY_ID);
        }
    }

    @Nested
    @DisplayName("AI 응답이 비어 있으면")
    class WhenAiReturnsEmpty {

        @Test
        @DisplayName("AI_RESPONSE_INVALID 예외를 던진다")
        void 예외를_던진다() {
            givenHobbyAndChatClient();
            given(callResponseSpec.entity(FastAPIRecommendResDto.class))
                    .willReturn(new FastAPIRecommendResDto(List.of()));

            assertThatThrownBy(() -> sut.requestActivityRecommendAI(user, hobby))
                    .isInstanceOf(CustomException.class);
            verify(aiCallLogService).record(any());
        }

        @Test
        @DisplayName("AI 응답이 null이어도 AI_RESPONSE_INVALID 예외를 던진다")
        void null_응답도_예외를_던진다() {
            givenHobbyAndChatClient();
            given(callResponseSpec.entity(FastAPIRecommendResDto.class)).willReturn(null);

            assertThatThrownBy(() -> sut.requestActivityRecommendAI(user, hobby))
                    .isInstanceOf(CustomException.class);
            verify(aiCallLogService).record(any());
        }
    }
}
