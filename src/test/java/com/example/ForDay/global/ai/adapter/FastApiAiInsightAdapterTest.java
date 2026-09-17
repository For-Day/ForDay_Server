package com.example.ForDay.global.ai.adapter;

import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.ai.chat.client.ChatClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FastApiAiInsightAdapter - Spring AI 활동 요약")
class FastApiAiInsightAdapterTest {

    private static final String USER_ID = "user-1";
    private static final Long HOBBY_ID = 10L;
    private static final String HOBBY_NAME = "독서";

    @Mock private ChatClient chatClient;
    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec callResponseSpec;
    @Mock private ActivityRecordRepository activityRecordRepository;
    @Mock private ActivityRecord activityRecord;

    private FastApiAiInsightAdapter sut;

    private void givenChatClient() {
        sut = new FastApiAiInsightAdapter(chatClient, activityRecordRepository);

        given(chatClient.prompt()).willReturn(requestSpec);
        given(requestSpec.user(anyString())).willReturn(requestSpec);
        given(requestSpec.call()).willReturn(callResponseSpec);
    }

    @Nested
    @DisplayName("최근 기록으로 AI 요약을 정상적으로 받아오면")
    class WhenAiReturnsSummary {

        @Test
        @DisplayName("앞뒤 공백을 제거하고 반환한다")
        void 요약을_반환한다() {
            givenChatClient();
            given(activityRecordRepository.findRecentByUserIdAndHobbyId(anyString(), anyLong(), any(LocalDateTime.class)))
                    .willReturn(List.of(activityRecord));
            given(activityRecord.getCreatedAt()).willReturn(LocalDateTime.now());
            given(activityRecord.getMemo()).willReturn("아침 독서 30분");
            given(callResponseSpec.content()).willReturn("  주로 아침시간에 독서활동을 하셨네요!  ");

            String result = sut.requestActivitySummary(USER_ID, HOBBY_ID, HOBBY_NAME);

            assertThat(result).isEqualTo("주로 아침시간에 독서활동을 하셨네요!");
        }
    }

    @Nested
    @DisplayName("AI 호출이 실패하면")
    class WhenAiCallFails {

        @Test
        @DisplayName("예외를 던지지 않고 빈 문자열을 반환한다")
        void 빈_문자열을_반환한다() {
            givenChatClient();
            given(activityRecordRepository.findRecentByUserIdAndHobbyId(anyString(), anyLong(), any(LocalDateTime.class)))
                    .willReturn(List.of());
            given(callResponseSpec.content()).willThrow(new RuntimeException("AI 서버 오류"));

            String result = sut.requestActivitySummary(USER_ID, HOBBY_ID, HOBBY_NAME);

            assertThat(result).isEmpty();
        }
    }
}
