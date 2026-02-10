package com.example.ForDay.domain.hobby.dto.response;

import com.example.ForDay.domain.hobby.type.ExtensionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetHobbyExtensionResDto {
    private Long hobbyId;
    private ExtensionType type;
    private String message;
}
