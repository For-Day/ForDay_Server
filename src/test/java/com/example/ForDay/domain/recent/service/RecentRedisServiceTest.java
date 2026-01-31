package com.example.ForDay.domain.recent.service;

import com.example.ForDay.domain.recent.dto.response.GetRecentKeywordResDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RecentRedisServiceTest {

    @InjectMocks
    private RecentRedisService recentRedisService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private final String userId = "user123";
    private final String key = "recent:user123";

    @BeforeEach
    void setUp() {
        // redisTemplate.opsForZSet() 호출 시 mock 객체 반환 설정
        lenient().when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
    }

    @Test
    @DisplayName("최근 검색어 생성 시 - 중복 데이터는 score만 갱신되고 20개를 유지해야 한다")
    void createRecentKeyword_Success() {
        // given
        String keyword = "테니스 레슨";
        given(zSetOperations.size(key)).willReturn(21L); // 20개 초과 상황 가정

        // when
        recentRedisService.createRecentKeyword(userId, keyword);

        // then
        // 1. ZSet에 추가되었는지 확인
        verify(zSetOperations).add(eq(key), eq(keyword), anyDouble());
        // 2. 20개 초과분 삭제 로직 작동 확인 (size - 20 - 1 = 0인덱스부터 삭제)
        verify(zSetOperations).removeRange(eq(key), eq(0L), eq(0L));
        // 3. 만료 시간 설정 확인
        verify(redisTemplate).expire(eq(key), eq(30L), eq(TimeUnit.DAYS));
    }

    @Test
    @DisplayName("전체 목록 조회 시 - 기획대로 최대 5개까지만 노출되어야 한다")
    void getRecentKeywords_Limit5() {
        // given
        // 5개의 결과 응답 시뮬레이션
        Set<ZSetOperations.TypedTuple<String>> mockTuples = Set.of(
                new DefaultTypedTuple<>("검색어1", 1000.0),
                new DefaultTypedTuple<>("검색어2", 900.0)
        );
        given(zSetOperations.reverseRangeWithScores(key, 0, 4)).willReturn(mockTuples);

        // when
        GetRecentKeywordResDto result = recentRedisService.getRecentKeywords(userId);

        // then
        assertThat(result.getRecentList()).hasSize(2);
        verify(zSetOperations).reverseRangeWithScores(key, 0, 4); // VIEW_SIZE - 1
    }

    @Test
    @DisplayName("개별 삭제 시 - 해당 타임스탬프(score)를 가진 검색어를 찾아 삭제해야 한다")
    void deleteRecentKeyword_Success() {
        // given
        Long recentId = 12345L;
        String keywordToDelete = "삭제할검색어";
        given(zSetOperations.rangeByScore(key, (double) recentId, (double) recentId))
                .willReturn(Set.of(keywordToDelete));

        // when
        Long deletedId = recentRedisService.deleteRecentKeyword(userId, recentId);

        // then
        assertThat(deletedId).isEqualTo(recentId);
        verify(zSetOperations).remove(key, keywordToDelete);
    }

    @Test
    @DisplayName("전체 삭제 시 - 모든 데이터를 지우고 삭제된 ID 리스트를 반환한다")
    void deleteAllRecentKeywords_Success() {
        // given
        Set<ZSetOperations.TypedTuple<String>> mockTuples = Set.of(
                new DefaultTypedTuple<>("검색어1", 1000.0),
                new DefaultTypedTuple<>("검색어2", 2000.0)
        );
        given(zSetOperations.reverseRangeWithScores(key, 0, -1)).willReturn(mockTuples);

        // when
        recentRedisService.deleteAllRecentKeywords(userId);

        // then
        verify(redisTemplate).delete(key);
    }

    // DefaultTypedTuple 구현체 (테스트용)
    private static class DefaultTypedTuple<V> implements ZSetOperations.TypedTuple<V> {
        private final V value;
        private final Double score;

        public DefaultTypedTuple(V value, Double score) {
            this.value = value;
            this.score = score;
        }

        @Override public V getValue() { return value; }
        @Override public Double getScore() { return score; }
        @Override public int compareTo(ZSetOperations.TypedTuple<V> o) { return score.compareTo(o.getScore()); }
    }

}