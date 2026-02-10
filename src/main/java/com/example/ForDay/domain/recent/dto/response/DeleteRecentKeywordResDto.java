package com.example.ForDay.domain.recent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeleteRecentKeywordResDto {
    private String message;
    private Long recentId;
}
