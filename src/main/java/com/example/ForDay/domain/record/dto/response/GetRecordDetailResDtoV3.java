package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.dto.ReactionSummary;
import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.entity.RecordImage;
import com.example.ForDay.domain.record.type.RecordReactionType;
import com.example.ForDay.domain.record.type.RecordVisibility;
import com.example.ForDay.global.util.TimeUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "기록 상세 조회 응답 DTO V3")
public class GetRecordDetailResDtoV3 {

    @Schema(description = "취미 ID", example = "1")
    private Long hobbyId;

    @Schema(description = "취미 이름", example = "독서")
    private String hobbyName;

    @Schema(description = "활동 ID", example = "10")
    private Long activityId;

    @Schema(description = "활동 내용", example = "오늘의 독서")
    private String activityContent;

    @Schema(description = "활동 기록 ID", example = "42")
    private Long activityRecordId;

    @Schema(description = "첨부 이미지 목록")
    private List<ImageInfo> images;

    @Schema(description = "스티커 이름", example = "HAPPY")
    private String sticker;

    @Schema(description = "기록 생성 일시", example = "2024.06.16 오후 3:00")
    private String createdAt;

    @Schema(description = "메모", example = "오늘은 1시간 독서했다.")
    private String memo;

    @Schema(description = "본인 기록 여부", example = "true")
    private boolean recordOwner;

    @Schema(description = "스크랩 여부", example = "false")
    private boolean scraped;

    @Schema(description = "작성자 정보")
    private UserInfoDto userInfo;

    @Schema(description = "공개 범위 (PUBLIC / FRIEND / PRIVATE)", example = "PUBLIC")
    private RecordVisibility visibility;

    @Schema(description = "읽지 않은 반응 알림 여부 (기록 작성자에게만 의미 있음)")
    private NewReactionDto newReaction;

    @Schema(description = "현재 사용자의 반응 클릭 여부")
    private UserReactionDto userReaction;

    @Schema(description = "이전 기록 ID (없으면 null)", example = "41")
    private Long prevRecordId;

    @Schema(description = "다음 기록 ID (없으면 null)", example = "43")
    private Long nextRecordId;


    public static GetRecordDetailResDtoV3 of(RecordDetailQueryDto detail,
                                             boolean isRecordOwner,
                                             boolean scraped,
                                             List<ReactionSummary> summaries,
                                             List<RecordImage> images,
                                             String profileImageUrl,
                                             String readerId) {
        return GetRecordDetailResDtoV3.builder()
                .hobbyId(detail.hobbyId())
                .hobbyName(detail.hobbyName())
                .activityId(detail.activityId())
                .activityContent(detail.activityContent())
                .activityRecordId(detail.recordId())
                .images(images.stream().map(ImageInfo::from).toList())
                .sticker(detail.sticker())
                .createdAt(TimeUtil.formatLocalDateTime(detail.createdAt()))
                .memo(detail.memo())
                .recordOwner(isRecordOwner)
                .scraped(scraped)
                .userInfo(UserInfoDto.of(detail, profileImageUrl))
                .visibility(detail.visibility())
                .newReaction(NewReactionDto.of(summaries, isRecordOwner))
                .userReaction(UserReactionDto.of(summaries, readerId))
                .prevRecordId(null)
                .nextRecordId(null)
                .build();
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    @Schema(description = "첨부 이미지 정보")
    public static class ImageInfo {

        @Schema(description = "이미지 ID", example = "7")
        private Long imageId;

        @Schema(description = "이미지 URL", example = "https://s3.example.com/image.jpg")
        private String imageUrl;

        @Schema(description = "이미지 순서", example = "1")
        private Integer imageOrder;

        @Schema(description = "이미지 가로 크기 (px)", example = "1080")
        private Long imageWidth;

        @Schema(description = "이미지 세로 크기 (px)", example = "1920")
        private Long imageHeight;

        @Schema(description = "썸네일 여부 (imageOrder=1인 이미지가 true)", example = "true")
        private boolean thumbnail;

        public static ImageInfo from(RecordImage image) {
            return ImageInfo.builder()
                    .imageId(image.getId())
                    .imageUrl(image.getImageUrl())
                    .imageOrder(image.getImageOrder())
                    .imageWidth(image.getImageWidth())
                    .imageHeight(image.getImageHeight())
                    .thumbnail(image.isThumbnail())
                    .build();
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "읽지 않은 반응 알림 여부")
    public static class NewReactionDto {

        @Schema(description = "읽지 않은 AWESOME 반응 존재 여부", example = "true")
        private boolean newAweSome;

        @Schema(description = "읽지 않은 GREAT 반응 존재 여부", example = "false")
        private boolean newGreat;

        @Schema(description = "읽지 않은 AMAZING 반응 존재 여부", example = "false")
        private boolean newAmazing;

        @Schema(description = "읽지 않은 FIGHTING 반응 존재 여부", example = "false")
        private boolean newFighting;

        public static NewReactionDto of(List<ReactionSummary> summaries, boolean isOwner) {
            if (!isOwner) return new NewReactionDto(false, false, false, false);
            List<RecordReactionType> unreadTypes = summaries.stream()
                    .filter(s -> !s.readWriter())
                    .map(ReactionSummary::type)
                    .distinct()
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
    @Schema(description = "현재 사용자의 반응 클릭 여부")
    public static class UserReactionDto {

        @Schema(description = "AWESOME 반응 클릭 여부", example = "true")
        private boolean pressedAweSome;

        @Schema(description = "GREAT 반응 클릭 여부", example = "false")
        private boolean pressedGreat;

        @Schema(description = "AMAZING 반응 클릭 여부", example = "false")
        private boolean pressedAmazing;

        @Schema(description = "FIGHTING 반응 클릭 여부", example = "false")
        private boolean pressedFighting;

        public static UserReactionDto of(List<ReactionSummary> summaries, String readerId) {
            List<RecordReactionType> myTypes = summaries.stream()
                    .filter(s -> s.reactedUserId().equals(readerId))
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
    @Schema(description = "작성자 정보")
    public static class UserInfoDto {

        @Schema(description = "작성자 유저 ID", example = "user-abc123")
        private String userId;

        @Schema(description = "작성자 닉네임", example = "포데이유저")
        private String nickname;

        @Schema(description = "작성자 프로필 이미지 URL", example = "https://s3.example.com/profile.jpg")
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
