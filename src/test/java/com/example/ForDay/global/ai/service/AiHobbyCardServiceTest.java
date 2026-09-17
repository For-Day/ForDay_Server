package com.example.ForDay.global.ai.service;

import com.example.ForDay.domain.activity.dto.response.FastAPIHobbyCardResDto;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.record.dto.HobbyCardActivityStatDto;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AiHobbyCardService - Spring AI 취미 카드 문구 생성")
class AiHobbyCardServiceTest {

    private static final Long HOBBY_ID = 10L;

    @Mock private ChatClient chatClient;
    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec callResponseSpec;
    @Mock private ActivityRecordRepository activityRecordRepository;
    @Mock private Hobby hobby;

    private AiHobbyCardService sut;

    private void given공통() {
        sut = new AiHobbyCardService(chatClient, activityRecordRepository);
        given(hobby.getId()).willReturn(HOBBY_ID);
        given(hobby.getHobbyName()).willReturn("독서");
    }

    @Nested
    @DisplayName("활동 기록 통계가 없으면")
    class WhenNoStats {

        @Test
        @DisplayName("AI를 호출하지 않고 기본 문구를 반환한다")
        void 기본_문구를_반환한다() {
            given공통();
            given(activityRecordRepository.findTopActivityStatsByHobbyId(anyLong(), anyLong()))
                    .willReturn(List.of());

            FastAPIHobbyCardResDto result = sut.requestHobbyCardContentAI(hobby);

            assertThat(result.getContent()).isEqualTo("꾸준히 쌓아온 기록들이 멋진 결실을 맺었네요!");
            verify(chatClient, never()).prompt();
        }
    }

    @Nested
    @DisplayName("활동 기록 통계가 있으면")
    class WhenStatsExist {

        @Test
        @DisplayName("AI가 생성한 문구를 앞뒤 공백 없이 반환한다")
        void AI_문구를_반환한다() {
            given공통();
            given(activityRecordRepository.findTopActivityStatsByHobbyId(anyLong(), anyLong()))
                    .willReturn(List.of(new HobbyCardActivityStatDto(1L, "아침 독서 30분", 5L)));
            given(activityRecordRepository.findRecordHoursByActivityIds(anyList()))
                    .willReturn(List.of(8, 9, 7));

            given(chatClient.prompt()).willReturn(requestSpec);
            given(requestSpec.user(anyString())).willReturn(requestSpec);
            given(requestSpec.call()).willReturn(callResponseSpec);
            given(callResponseSpec.content()).willReturn("  주로 아침시간을 활용한 독서활동  ");

            FastAPIHobbyCardResDto result = sut.requestHobbyCardContentAI(hobby);

            assertThat(result.getContent()).isEqualTo("주로 아침시간을 활용한 독서활동");
        }
    }
}
