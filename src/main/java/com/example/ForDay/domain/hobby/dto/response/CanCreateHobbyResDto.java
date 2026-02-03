package com.example.ForDay.domain.hobby.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CanCreateHobbyResDto {
    private String message;
    private boolean availability;
}
