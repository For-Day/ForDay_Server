package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.domain.hobby.type.ExtensionType;
import com.example.ForDay.global.common.response.message.HobbySuccessCode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.example.ForDay.global.common.response.message.HobbySuccessCode.SET_HOBBY_EXTENSION_SUCCESS;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetHobbyExtensionResDto {
    private Long hobbyId;
    private ExtensionType type;
    private String message;

    public static SetHobbyExtensionResDto of(Long hobbyId, ExtensionType type) {
        return new SetHobbyExtensionResDto(hobbyId, type, HobbySuccessCode.SET_HOBBY_EXTENSION_SUCCESS.getMessage());
    }
}
