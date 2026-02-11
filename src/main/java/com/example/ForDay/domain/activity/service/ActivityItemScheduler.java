package com.example.ForDay.domain.activity.service;

import com.example.ForDay.domain.activity.repository.ActivityRecommendItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityItemScheduler {

    private final ActivityRecommendItemRepository activityRecommendItemRepository;

    /**
     * 매일 새벽 3시에 실행 (cron: 초 분 시 일 월 요일)
     * 오늘 기준으로 이틀(48시간) 전 생성된 아이템 삭제
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void deleteOldRecommendItems() {
        // 기준 시간 계산: 현재 시간 - 2일
        LocalDateTime targetDate = LocalDateTime.now().minusDays(2);

        log.info("[Scheduler] 추천 아이템 삭제 시작 (기준 시간: {})", targetDate);

        try {
            activityRecommendItemRepository.deleteOldItems(targetDate);
            log.info("[Scheduler] 추천 아이템 삭제 완료");
        } catch (Exception e) {
            log.error("[Scheduler] 삭제 중 오류 발생: {}", e.getMessage());
        }
    }
}
