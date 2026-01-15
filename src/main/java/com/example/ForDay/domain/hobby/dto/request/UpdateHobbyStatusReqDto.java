package com.example.ForDay.domain.hobby.dto.request;

import com.example.ForDay.domain.hobby.type.HobbyStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHobbyStatusReqDto {
    private HobbyStatus hobbyStatus;
}
