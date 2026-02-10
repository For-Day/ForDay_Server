package com.example.ForDay.domain.user.service;

import com.example.ForDay.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuestCleanUpService {

    private final UserRepository userRepository;

    // 매일 새벽 3시
    @Scheduled(cron = "0 0 3 * * *")
    public void deleteOldGuests() {

        LocalDateTime sixMonthsAgo =
                LocalDateTime.now().minusMonths(12);

        int deleted = userRepository.deleteOldGuests(sixMonthsAgo);

        log.info("[Guest Cleanup] 12개월 이상 활동 없는 게스트 삭제 = {}", deleted);
    }
}
