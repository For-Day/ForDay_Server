package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.record.repository.ActivityRecordRepository;
import com.example.ForDay.domain.user.entity.User;
import com.example.ForDay.global.port.AiInsightPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 포트 도입의 효과를 보여주는 테스트.
 *
 * <p>{@code @SpringBootTest}도, 실제 Redis도, FastAPI 서버도 없이 도메인 규칙만 검증한다.
 * 이전에는 이 로직이 {@code global/ai/service/AiUserSummaryService} 안에서 RestTemplate에
 * 직접 묶여 있어 이런 식으로 떼어낼 수 없었다.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("HobbyAiSummaryService - AI 요약 생성 판단")
class HobbyAiSummaryServiceTest {

    private static final String USER_ID = "user-1";
    private static final String SOCIAL_ID = "social-1";
    private static final Long HOBBY_ID = 10L;
    private static final String HOBBY_NAME = "독서";

    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private ActivityRecordRepository activityRecordRepository;
    @Mock private AiInsightPort aiInsightPort;
    @Mock private User user;
    @Mock private Hobby hobby;

    @InjectMocks private HobbyAiSummaryService sut;

    private void givenRecordCount(long count) {
        given(user.getId()).willReturn(USER_ID);
        given(user.getSocialId()).willReturn(SOCIAL_ID);
        given(hobby.getId()).willReturn(HOBBY_ID);
        given(hobby.getHobbyName()).willReturn(HOBBY_NAME);
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(activityRecordRepository.countByUserIdAndHobbyIdAndCreatedAtAfterAndDeletedFalse(
                anyString(), anyLong(), any(LocalDateTime.class))).willReturn(count);
    }

    @Nested
    @DisplayName("최근 7일 기록이 5건 미만이면")
    class WhenRecordsAreInsufficient {

        @Test
        @DisplayName("빈 문자열을 반환한다")
        void 빈_문자열을_반환한다() {
            givenRecordCount(4);

            assertThat(sut.determine(user, hobby)).isEmpty();
        }

        @Test
        @DisplayName("AI 서버를 호출하지 않는다")
        void AI_서버를_호출하지_않는다() {
            givenRecordCount(4);

            sut.determine(user, hobby);

            verify(aiInsightPort, never()).requestActivitySummary(anyString(), anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("캐시된 요약이 있으면")
    class WhenSummaryIsCached {

        @Test
        @DisplayName("캐시 값을 그대로 반환하고 AI 서버를 호출하지 않는다")
        void 캐시를_그대로_쓴다() {
            givenRecordCount(5);
            given(redisTemplate.hasKey(anyString())).willReturn(true);
            given(valueOperations.get(anyString())).willReturn("캐시된 요약");

            assertThat(sut.determine(user, hobby)).isEqualTo("캐시된 요약");
            verify(aiInsightPort, never()).requestActivitySummary(anyString(), anyLong(), anyString());
        }
    }

    @Nested
    @DisplayName("캐시가 없고 기록이 충분하면")
    class WhenSummaryMustBeCreated {

        @Test
        @DisplayName("AI 서버에서 받아온 요약을 반환하고 캐시에 저장한다")
        void AI_요약을_받아_저장한다() {
            givenRecordCount(5);
            given(redisTemplate.hasKey(anyString())).willReturn(false);
            given(aiInsightPort.requestActivitySummary(USER_ID, HOBBY_ID, HOBBY_NAME))
                    .willReturn("새로 만든 요약");

            assertThat(sut.determine(user, hobby)).isEqualTo("새로 만든 요약");
            verify(valueOperations).set(anyString(), eq("새로 만든 요약"), anyLong(), any(TimeUnit.class));
        }

        @Test
        @DisplayName("AI 응답이 비어 있으면 캐시에 저장하지 않는다")
        void 빈_응답은_캐시하지_않는다() {
            givenRecordCount(5);
            given(redisTemplate.hasKey(anyString())).willReturn(false);
            given(aiInsightPort.requestActivitySummary(anyString(), anyLong(), anyString()))
                    .willReturn("");

            assertThat(sut.determine(user, hobby)).isEmpty();
            verify(valueOperations, never()).set(anyString(), any(), anyLong(), any(TimeUnit.class));
        }
    }
}
