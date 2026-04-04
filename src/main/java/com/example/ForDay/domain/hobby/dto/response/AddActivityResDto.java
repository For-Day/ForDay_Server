package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.global.common.response.message.HobbySuccessCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.HobbySuccessCode.ADD_ACTIVITY_SUCCESS;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddActivityResDto {
    private String message;
    private Integer createdActivityNum;

    public static AddActivityResDto of(Integer createdActivityNum) {
        return new AddActivityResDto(
                HobbySuccessCode.ADD_ACTIVITY_SUCCESS.getMessage(),
                createdActivityNum
        );
    }
}
