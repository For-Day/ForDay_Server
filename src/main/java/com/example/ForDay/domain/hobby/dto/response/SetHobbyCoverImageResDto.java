package com.example.ForDay.domain.hobby.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetHobbyCoverImageResDto {
    private String message;
    private Long hobbyId;
    private Long recordId;
    private String coverImageUrl;
}
