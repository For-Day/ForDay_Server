package com.example.ForDay.domain.hobby.dto.response;

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
}
