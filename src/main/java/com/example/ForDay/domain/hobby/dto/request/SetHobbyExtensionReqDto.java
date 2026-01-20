package com.example.ForDay.domain.hobby.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetHobbyExtensionReqDto {
    private ExtensionType type;
}
