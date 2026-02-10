package com.example.ForDay.domain.record.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportActivityRecordResDto {
    private Long recordId;
    private String recordWriterId;
    private String recordWriterNickname;
    private String message;
}
