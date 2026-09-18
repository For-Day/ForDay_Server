package com.example.ForDay.global.ai.service;

import com.example.ForDay.global.ai.document.AiCallLog;
import com.example.ForDay.global.ai.repository.AiCallLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 이슈 #407 - {@link AiCallLog} 저장을 전담한다.
 *
 * <p>MongoDB 저장 실패(접속 불가 등)를 여기서 흡수해 AI 응답 흐름에 영향을
 * 주지 않는다 - {@code FastApiAiInsightAdapter}가 AI 호출 실패를 삼키는 것과
 * 같은 이유다. 이력은 감사/분석용 부가 데이터이지, 있어야 응답이 나가는
 * 필수 경로가 아니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCallLogService {

    private final AiCallLogRepository aiCallLogRepository;

    public void record(AiCallLog aiCallLog) {
        try {
            aiCallLogRepository.save(aiCallLog);
        } catch (Exception e) {
            log.error("AI 호출 이력 저장 실패 | callType: {}, userId: {}, hobbyId: {}, error: {}",
                    aiCallLog.getCallType(), aiCallLog.getUserId(), aiCallLog.getHobbyId(), e.getMessage());
        }
    }
}
