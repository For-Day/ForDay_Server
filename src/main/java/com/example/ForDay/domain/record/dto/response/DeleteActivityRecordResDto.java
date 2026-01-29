package com.example.ForDay.domain.record.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteActivityRecordResDto {
    private String message;
    private Long recordId;
}
