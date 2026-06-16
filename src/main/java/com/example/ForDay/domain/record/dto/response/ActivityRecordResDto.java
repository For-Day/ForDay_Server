package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.domain.record.entity.RecordImage;
import com.example.ForDay.domain.record.type.RecordVisibility;
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
public class ActivityRecordResDto {

    private Long activityRecordId;
    private String hobbyName;
    private String activityContent;
    private String sticker;
    private String memo;
    private RecordVisibility visibility;
    private List<ImageInfo> images;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImageInfo {
        private Long imageId;
        private String imageUrl;
        private Long imageWidth;
        private Long imageHeight;
        private Integer imageOrder;
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