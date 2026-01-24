package com.example.ForDay.domain.record.dto.response;

import com.example.ForDay.domain.record.type.RecordVisibility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRecordVisibilityResDto {
    private String message;
    private RecordVisibility previousVisibility;
    private RecordVisibility newVisibility;
}
