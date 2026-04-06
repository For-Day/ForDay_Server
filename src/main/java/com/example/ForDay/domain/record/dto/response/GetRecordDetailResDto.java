package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.dto.ReactionSummary;
import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.global.util.TimeUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "기록 상세 조회 응답 DTO")
public class GetRecordDetailResDto {
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

    public static GetRecordDetailResDto of(RecordDetailQueryDto detail,
                                           boolean isOwner,
                                           boolean scraped,
                                           NewReactionDto newReaction,
                                           UserReactionDto userReaction,
                                           String profileImageUrl) {
        return GetRecordDetailResDto.builder()
                .hobbyId(detail.hobbyId())
                .hobbyName(detail.hobbyName())
                .activityId(detail.activityId())
                .activityContent(detail.activityContent())
                .activityRecordId(detail.recordId())
                .imageUrl(detail.imageUrl())
                .sticker(detail.sticker())
                .createdAt(TimeUtil.formatLocalDateTime(detail.createdAt()))
                .memo(detail.memo())
                .recordOwner(isOwner)
                .scraped(scraped)
                .userInfo(UserInfoDto.of(detail, profileImageUrl))
                .visibility(detail.visibility())
                .newReaction(newReaction)
                .userReaction(userReaction)
                .build();
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "새로운 리액션 알림 여부")
    public static class NewReactionDto {
        private boolean newAweSome;
        private boolean newGreat;
        private boolean newAmazing;
        private boolean newFighting;

        public static NewReactionDto of(List<ReactionSummary> summaries, boolean isOwner) {
            if (!isOwner) return new NewReactionDto(false, false, false, false);

            List<RecordReactionType> unreadTypes = summaries.stream()
                    .filter(s -> !s.readWriter())
                    .map(ReactionSummary::type)
                    .toList();
            return new NewReactionDto(
                    unreadTypes.contains(RecordReactionType.AWESOME),
                    unreadTypes.contains(RecordReactionType.GREAT),
                    unreadTypes.contains(RecordReactionType.AMAZING),
                    unreadTypes.contains(RecordReactionType.FIGHTING)
            );
        }
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

        public static UserReactionDto of(List<ReactionSummary> summaries, String userId) {
            List<RecordReactionType> myTypes = summaries.stream()
                    .filter(s -> s.reactedUserId().equals(userId))
                    .map(ReactionSummary::type)
                    .toList();
            return new UserReactionDto(
                    myTypes.contains(RecordReactionType.AWESOME),
                    myTypes.contains(RecordReactionType.GREAT),
                    myTypes.contains(RecordReactionType.AMAZING),
                    myTypes.contains(RecordReactionType.FIGHTING)
            );
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UserInfoDto {
        private String userId;
        private String nickname;
        private String profileImageUrl;

        public static UserInfoDto of(RecordDetailQueryDto detail, String profileImageUrl) {
            return UserInfoDto.builder()
                    .userId(detail.writerId())
                    .nickname(detail.writerNickname())
                    .profileImageUrl(profileImageUrl)
                    .build();
        }
    }
}