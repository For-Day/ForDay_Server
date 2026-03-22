package com.example.ForDay.domain.hobby.service;

import com.example.ForDay.domain.hobby.dto.AiInsightResult;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class HobbyAiInsightService {

    private final AiCallCountService aiCallCountService;
    private final UserSummaryAIService aiSummaryService; // determineAiSummary 이동

    @Value("${ai.max-call-limit}")
    private int maxCallLimit;

    public AiInsightResult resolveInsight(User user, Hobby hobby) {
        int currentCount = aiCallCountService.getCurrentCount(user.getSocialId(), hobby.getId());
        int remaining = maxCallLimit - currentCount;
        boolean isCallRemaining = remaining > 0;

        String summaryText = aiSummaryService.determine(user, hobby);

        return new AiInsightResult(summaryText, isCallRemaining, remaining);
    }
}