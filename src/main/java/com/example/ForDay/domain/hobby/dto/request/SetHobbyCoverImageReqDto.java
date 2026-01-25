package com.example.ForDay.domain.hobby.dto.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetHobbyCoverImageReqDto {
    private Long hobbyId;
    private Long recordId;
    private String coverImageUrl;
}
