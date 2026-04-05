package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.dto.ReportActivityRecordDto;
import com.example.ForDay.global.common.response.message.RecordSuccessCode;
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

    public static ReportActivityRecordResDto from(ReportActivityRecordDto record) {
        return new ReportActivityRecordResDto(
                record.getRecordId(),
                record.getWriterId(),
                record.getWriterNickname(),
                RecordSuccessCode.REPORT_RECORD_SUCCESS.getMessage()
        );
    }
}
