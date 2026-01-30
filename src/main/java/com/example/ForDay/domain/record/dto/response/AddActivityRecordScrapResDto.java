package com.example.ForDay.domain.record.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AddActivityRecordScrapResDto {
    private String message;
    private Long recordId;
    private boolean scraped;
}
