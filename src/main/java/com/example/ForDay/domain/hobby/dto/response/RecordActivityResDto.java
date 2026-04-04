package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.domain.activity.entity.Activity;
import com.example.ForDay.domain.hobby.entity.Hobby;
import com.example.ForDay.domain.record.entity.ActivityRecord;
import com.example.ForDay.global.common.response.message.ActivitySuccessCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecordActivityResDto {
    private String message;
    private Long hobbyId;
    private Long activityRecordId;
    private String activityContent;
    private String imageUrl;
    private String sticker;
    private String memo;
    private boolean extensionCheckRequired; // 취미 연장 여부 확인이 필요한지

    public static RecordActivityResDto of(
            Hobby hobby,
            ActivityRecord activityRecord,
            Activity activity,
            String sticker,
            boolean extensionCheckRequired) {
        return new RecordActivityResDto(
                ActivitySuccessCode.RECORD_ACTIVITY_SUCCESS.getMessage(),
                hobby.getId(),
                activityRecord.getId(),
                activity.getContent(),
                activityRecord.getImageUrl(),
                sticker,
                activityRecord.getMemo(),
                extensionCheckRequired
        );
    }
}
