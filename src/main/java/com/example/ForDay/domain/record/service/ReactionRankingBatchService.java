package com.example.ForDay.domain.record.service;

import com.example.ForDay.domain.record.dto.ReactionCountDto;
import com.example.ForDay.domain.record.repository.ActivityRecordReactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReactionRankingBatchService {

    private final ActivityRecordReactionRepository reactionRepository;
    private final RedisTemplate<String, Object> redisObjectTemplate;
    private static final String RANKING_KEY = "reaction:ranking:30days";

    @Scheduled(cron = "0 5 0 * * *")
    public void update30DaysRanking() {
        LocalDate targetDate = LocalDate.now().minusDays(31);
        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(LocalTime.MAX);

        log.info("30일 랭킹 업데이트 배치 시작: 대상 날짜 {}", targetDate);

        List<ReactionCountDto> expiredCounts = reactionRepository.countReactionsByDate(start, end);

        if (expiredCounts.isEmpty()) {
            log.info("차감할 과거 반응 데이터가 없습니다.");
            return;
        }

        for (ReactionCountDto dto : expiredCounts) {
            // incrementScore에 음수(-count)를 넣으면 값이 줄어듭니다.
            redisObjectTemplate.opsForZSet().incrementScore(
                    RANKING_KEY,
                    dto.getRecordId().toString(),
                    -dto.getCount()
            );
        }

        redisObjectTemplate.opsForZSet().removeRangeByScore(RANKING_KEY, Double.NEGATIVE_INFINITY, 0);

        log.info("랭킹 업데이트 완료. 총 {}개의 기록 점수 조정됨.", expiredCounts.size());
    }
}