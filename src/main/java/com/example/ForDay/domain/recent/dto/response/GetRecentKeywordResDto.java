package com.example.ForDay.domain.recent.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetRecentKeywordResDto {
    private List<RecentDto> recentList;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RecentDto{
        private Long recentId;
        private String keyword;
        private LocalDateTime createdAt;
    }
}
