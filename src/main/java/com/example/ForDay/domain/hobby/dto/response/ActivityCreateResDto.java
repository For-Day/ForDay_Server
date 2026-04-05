package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.global.common.response.message.HobbySuccessCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityCreateResDto {
    private String message;
    private Long hobbyId;

    public static ActivityCreateResDto of(Long hobbyId) {
        return new ActivityCreateResDto(HobbySuccessCode.CREATE_HOBBY_SUCCESS.getMessage(), hobbyId);
    }
}
