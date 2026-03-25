package com.example.ForDay.domain.hobby.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.HobbySuccessMessage.CREATE_HOBBY_SUCCESS;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActivityCreateResDto {
    private String message;
    private Long hobbyId;

    public static ActivityCreateResDto of(Long hobbyId) {
        return new ActivityCreateResDto(CREATE_HOBBY_SUCCESS, hobbyId);
    }
}
