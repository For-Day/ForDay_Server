package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.type.RecordVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetRecordDetailResDtoV2 {
    private Long hobbyId;
    private String hobbyName;
    private Long activityId;
    private String activityContent;
    private Long activityRecordId;
    private String imageUrl;
    private String sticker;
    private String createdAt;
    private String memo;
    private boolean recordOwner;
    private boolean scraped;
    private UserInfoDto userInfo;
    private RecordVisibility visibility;
    private NewReactionDto newReaction;
    private UserReactionDto userReaction;
    private Long prevRecordId;
    private Long nextRecordId;

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

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UserInfoDto {
        private String userId;
        private String nickname;
        private String profileImageUrl;
    }
}
