package com.example.ForDay.domain.record.dto.request;

import com.example.ForDay.domain.record.type.RecordVisibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityRecordReqDtoV2 {

    private Long hobbyId;

    private Long activityId;

    private String activityContent;

    private String sticker;

    @Valid
    private List<ActivityImageReqDto> images;

    @NotNull
    private RecordVisibility visibility;

    private String memo;

    @AssertTrue(message = "activityId가 null이면 activityContent는 필수입니다. activityId가 있으면 activityContent는 null이어야 합니다.")
    public boolean isActivityContentValid() {
        if (activityId == null) {
            return activityContent != null && !activityContent.isBlank();
        } else {
            return activityContent == null;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityImageReqDto {

        @NotBlank
        private String imageUrl;

        @NotNull
        private Integer imageOrder;

        @NotNull
        private Long imageWidth;

        @NotNull
        private Long imageHeight;
    }
}
