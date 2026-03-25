package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.domain.hobby.dto.CoverChangeResult;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.HobbySuccessMessage.SET_HOBBY_COVER_IMAGE_SUCCESS;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetHobbyCoverImageResDto {
    private String message;
    private Long hobbyId;
    private Long recordId;
    private String coverImageUrl;

    public static SetHobbyCoverImageResDto of(CoverChangeResult result, String coverImageUrl) {
        return new SetHobbyCoverImageResDto(
                SET_HOBBY_COVER_IMAGE_SUCCESS,
                result.hobbyId(),
                result.recordId(),
                coverImageUrl
        );
    }
}
