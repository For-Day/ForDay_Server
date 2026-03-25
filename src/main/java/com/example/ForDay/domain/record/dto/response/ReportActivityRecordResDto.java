package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.dto.RecordDetailQueryDto;
import com.example.ForDay.domain.record.dto.ReportActivityRecordDto;
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

    private static final String SUCCESS_MESSAGE = "기록이 정상적으로 신고되었습니다.";

    public static ReportActivityRecordResDto from(ReportActivityRecordDto record) {
        return new ReportActivityRecordResDto(
                record.getRecordId(),
                record.getWriterId(),
                record.getWriterNickname(),
                SUCCESS_MESSAGE
        );
    }
}
