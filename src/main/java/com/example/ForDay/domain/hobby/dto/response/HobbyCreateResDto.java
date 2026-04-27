package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.global.common.response.message.HobbySuccessCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class HobbyCreateResDto {
    private String message;
    private Long hobbyId;

    public static HobbyCreateResDto of(Long hobbyId) {
        return new HobbyCreateResDto(HobbySuccessCode.CREATE_HOBBY_SUCCESS.getMessage(), hobbyId);
    }
}
