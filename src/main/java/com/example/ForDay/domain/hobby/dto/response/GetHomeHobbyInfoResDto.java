package com.example.ForDay.domain.hobby.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class GetHomeHobbyInfoResDto {
    private List<InProgressHobbyDto> inProgressHobbies;
    private ActivityPreviewDto activityPreview;

    // 추가된 필드들
    private String greetingMessage;     // "반가워요, Nickname님! 👋"
    private String userSummaryText;     // AI가 분석한 요약 문구 (기록 5개 이상 시)
    private String recommendMessage;    // "포데이 AI가 알맞은 취미활동을 추천해드려요"
    private boolean aiCallRemaining;    // 오늘 AI 호출 가능 여부

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InProgressHobbyDto {
        private Long hobbyId;
        private String hobbyName;
        private boolean currentHobby;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityPreviewDto {
        private Long activityId;
        private String content;
        private boolean aiRecommended;
    }
}