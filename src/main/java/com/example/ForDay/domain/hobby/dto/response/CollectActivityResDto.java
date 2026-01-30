package com.example.ForDay.domain.hobby.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectActivityResDto {
    private Long hobbyId;
    private String hobbyName;
    private Long activityId;
    private String content;
    private String message;
}
