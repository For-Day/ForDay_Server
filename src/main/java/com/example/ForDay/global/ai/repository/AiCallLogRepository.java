package com.example.ForDay.global.ai.repository;

import com.example.ForDay.global.ai.document.AiCallLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AiCallLogRepository extends MongoRepository<AiCallLog, String> {

    // 추천 품질 분석/"왜 이 추천이 나갔는지" 추적용 조회. 아직 사용하는 API는
    // 없지만, 이력을 쌓는 목적 자체가 이 조회이므로 저장 시점에 함께 정의해둔다.
    List<AiCallLog> findByUserIdAndHobbyIdAndCreatedAtBetween(
            String userId, Long hobbyId, LocalDateTime from, LocalDateTime to);
}
