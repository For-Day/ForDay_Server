package com.example.ForDay.domain.activity.dto.request;

import com.example.ForDay.domain.record.type.RecordVisibility;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RecordActivityReqDtoV2(
        Long hobbyId,

        Long activityId,

        @Size(max = 20, message = "activityContent는 최대 20자까지 입력 가능합니다.")
        String activityContent,

        String sticker,

        @Size(max = 5, message = "이미지는 최대 5장까지만 업로드 가능합니다.")
        List<String> hobbyImages,

        @Size(max = 100, message = "memo는 최대 100자까지 입력 가능합니다.")
        String memo,

        RecordVisibility visibility
) {
    public RecordActivityReqDtoV2 {

        if (activityId != null && activityContent != null) {
            throw new IllegalArgumentException("activityId가 존재할 때 activityContent는 입력할 수 없습니다.");
        }

        if (activityId == null && activityContent == null) {
            throw new IllegalArgumentException("activityId가 없을 때 activityContent는 필수 입력 값입니다.");
        }
    }
}