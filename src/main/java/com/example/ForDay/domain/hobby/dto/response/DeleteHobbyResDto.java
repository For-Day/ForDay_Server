package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.global.common.response.message.HobbySuccessCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteHobbyResDto {
    private Long hobbyId;
    private String message;

    public static DeleteHobbyResDto of(Long hobbyId) {
        return new DeleteHobbyResDto(
                hobbyId,
                HobbySuccessCode.DELETE_HOBBY_SUCCESS.getMessage()
        );
    }
}
