package com.example.ForDay.domain.activity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FastAPIHobbyCardReqDto {
    private Long userHobbyId;
}
