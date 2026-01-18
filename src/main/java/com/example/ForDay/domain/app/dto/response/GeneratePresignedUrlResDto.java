package com.example.ForDay.domain.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GeneratePresignedUrlResDto {
    private String uploadUrl; // presigned PUT url
    private String fileUrl;   // 실제 접근/저장할 URL
    private int order;
}
