package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.RecordImage;
import com.example.ForDay.domain.record.type.RecordVisibility;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "활동 기록하기 응답 DTO")
public class ActivityRecordResDto {

    @Schema(description = "생성된 활동 기록 ID", example = "42")
    private Long activityRecordId;

    @Schema(description = "기록된 취미 이름", example = "독서")
    private String hobbyName;

    @Schema(description = "기록된 활동 내용", example = "오늘의 독서")
    private String activityContent;

    @Schema(description = "스티커 이름", example = "HAPPY")
    private String sticker;

    @Schema(description = "메모", example = "오늘은 1시간 독서했다.")
    private String memo;

    @Schema(description = "공개 범위 (PUBLIC / FRIEND / PRIVATE)", example = "PUBLIC")
    private RecordVisibility visibility;

    @Schema(description = "첨부 이미지 목록")
    private List<ImageInfo> images;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "첨부 이미지 정보")
    public static class ImageInfo {

        @Schema(description = "이미지 ID", example = "7")
        private Long imageId;

        @Schema(description = "이미지 URL", example = "https://s3.example.com/image.jpg")
        private String imageUrl;

        @Schema(description = "이미지 가로 크기 (px)", example = "1080")
        private Long imageWidth;

        @Schema(description = "이미지 세로 크기 (px)", example = "1920")
        private Long imageHeight;

        @Schema(description = "이미지 순서", example = "1")
        private Integer imageOrder;

        @Schema(description = "썸네일 여부 (imageOrder=1인 이미지가 true)", example = "true")
        private boolean thumbnail;

        public static ImageInfo from(RecordImage recordImage) {
            return ImageInfo.builder()
                    .imageId(recordImage.getId())
                    .imageUrl(recordImage.getImageUrl())
                    .imageWidth(recordImage.getImageWidth())
                    .imageHeight(recordImage.getImageHeight())
                    .imageOrder(recordImage.getImageOrder())
                    .thumbnail(recordImage.isThumbnail())
                    .build();
        }
    }

    public static ActivityRecordResDto from(ActivityRecord activityRecord, List<RecordImage> images) {
        return ActivityRecordResDto.builder()
                .activityRecordId(activityRecord.getId())
                .hobbyName(activityRecord.getHobby().getHobbyName())
                .activityContent(activityRecord.getActivity().getContent())
                .sticker(activityRecord.getSticker())
                .memo(activityRecord.getMemo())
                .visibility(activityRecord.getVisibility())
                .images(images.stream()
                        .map(ImageInfo::from)
                        .collect(Collectors.toList()))
                .build();
    }
}