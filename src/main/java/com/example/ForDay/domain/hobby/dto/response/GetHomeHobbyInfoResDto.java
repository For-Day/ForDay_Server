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
    private Integer aiCallRemainingCount; // 현재까지 호출한 횟수
    private String nickname;

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

    public static GetHomeHobbyInfoResDto of(
            GetHomeHobbyInfoResDto baseResponse,
            String nickname,
            String userSummaryText,
            boolean isAiCallRemaining,
            int remainingCount) {

        return baseResponse.toBuilder()
                .greetingMessage("반가워요, " + nickname + "님! 👋")
                .userSummaryText(userSummaryText)
                .recommendMessage("포데이 AI가 알맞은 취미활동을 추천해드려요")
                .aiCallRemaining(isAiCallRemaining)
                .aiCallRemainingCount(remainingCount)
                .nickname(nickname)
                .build();
    }

    public static GetHomeHobbyInfoResDto ofDefault(String nickname) {
        return new GetHomeHobbyInfoResDto(
                List.of(),
                null,
                "반가워요, " + nickname + "님! 👋",
                "",
                "포데이 AI가 알맞은 취미활동을 추천해드려요",
                false,
                0,
                null
        );
    }
}