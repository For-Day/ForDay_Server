package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.type.RecordVisibility;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "기록 상세 조회 응답 DTO")
public class GetRecordDetailResDto {
    private Long activityId;
    private String activityContent;
    private Long activityRecordId;
    private String imageUrl;
    private String sticker;
    private String createdAt;
    private String memo;
    private boolean recordOwner;
    private RecordVisibility visibility;
    private NewReactionDto newReaction;
    private UserReactionDto userReaction;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "새로운 리액션 알림 여부")
    public static class NewReactionDto {
        private boolean newAweSome;
        private boolean newGreat;
        private boolean newAmazing;
        private boolean newFighting;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "현재 사용자의 리액션 클릭 여부")
    public static class UserReactionDto {
        @Schema(description = "Awesome 리액션 클릭 여부", example = "true")
        private boolean pressedAweSome;

        @Schema(description = "Great 리액션 클릭 여부", example = "false")
        private boolean pressedGreat;

        @Schema(description = "Amazing 리액션 클릭 여부", example = "false")
        private boolean pressedAmazing;

        @Schema(description = "Fighting 리액션 클릭 여부", example = "true")
        private boolean pressedFighting;
    }
}